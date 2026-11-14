package com.example.foodtracker.data

import com.example.foodtracker.data.db.DailyWaterTotal
import com.example.foodtracker.data.db.FoodDao
import com.example.foodtracker.data.db.FoodEntity
import java.util.Locale
import com.example.foodtracker.data.db.FoodLogDao
import com.example.foodtracker.data.db.FoodLogEntity
import com.example.foodtracker.data.db.FoodLogWithFood
import com.example.foodtracker.data.db.IndianFoodSeed
import com.example.foodtracker.data.db.WaterLogDao
import com.example.foodtracker.data.db.WaterLogEntity
import com.example.foodtracker.data.db.WeightLogDao
import com.example.foodtracker.data.db.WeightLogEntity
import com.example.foodtracker.util.DateUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

/** Source identifier for online search results — shown to the user as a chip. */
enum class OnlineSource(val display: String) {
    OpenFoodFacts("OpenFoodFacts"),
    USDA("USDA");
}

/** A search hit annotated with the API it came from. */
data class OnlineSearchResult(val food: FoodEntity, val source: OnlineSource)

/** Where a barcode hit came from, so we can show it on the resolve panel. */
enum class BarcodeSource(val display: String) {
    OpenFoodFacts("OpenFoodFacts"),
    USDA("USDA"),
    Cached("Local cache"),
}

/**
 * Result of a barcode lookup attempt. Exactly one of [product] / [cached] is set
 * when found:
 *   - [product]: fresh hit from OFF/USDA that still has both bases — UI lets the
 *     user pick which to trust before saving.
 *   - [cached]: prior per-100g row from the local DB, surfaced only when every
 *     online source missed. Already normalised so no basis chip is needed.
 * Both null means the code wasn't recognised — fall through to manual entry.
 */
data class BarcodeLookup(
    val product: BarcodeProduct? = null,
    val cached: FoodEntity? = null,
    val source: BarcodeSource? = null,
)

class FoodRepository(
    private val foodDao: FoodDao,
    private val foodLogDao: FoodLogDao,
    private val waterLogDao: WaterLogDao,
    private val weightLogDao: WeightLogDao,
    private val api: OpenFoodFactsApi,
    private val usdaApi: UsdaFoodApi,
    private val aiApi: AiNutritionApi = AiNutritionApi(),
    /**
     * Called after any write that changes what the home-screen widgets show
     * (today's food log / activity). Wired in [com.example.foodtracker.FoodTrackerApp]
     * to push a [com.example.foodtracker.widget.WidgetUpdater] refresh. Kept as a
     * plain lambda so the data layer stays decoupled from the widget/UI layer.
     */
    private val onWidgetDataChanged: suspend () -> Unit = {},
) {

    // ----- Foods -----
    fun observeAllFoods(): Flow<List<FoodEntity>> = foodDao.observeAll()
    fun searchFoods(query: String): Flow<List<FoodEntity>> = foodDao.search(query)
    fun observeFavouriteFoods(): Flow<List<FoodEntity>> = foodDao.observeFavourites()
    suspend fun foodById(id: Long): FoodEntity? = foodDao.getById(id)
    suspend fun upsertFood(food: FoodEntity): Long = foodDao.upsert(food)

    /**
     * Opt-in AI nutrition lookup for the add-food form. Delegates to
     * [AiNutritionApi]; returns per-100 g [Nutrients] or null on any failure.
     * Only called when the user has enabled AI and supplied their own key.
     */
    suspend fun fetchAiNutrition(
        name: String,
        instructions: String,
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiNutrition? = aiApi.fetchNutrition(name, instructions, provider, model, apiKey)

    /** Reason for the most recent AI failure, or null. See [AiNutritionApi.lastError]. */
    fun lastAiError(): String? = aiApi.lastError

    /** Second-pass verification of a prior AI result. See [AiNutritionApi.verifyNutrition]. */
    suspend fun verifyAiNutrition(
        name: String,
        candidate: AiNutrition,
        instructions: String,
        provider: AiProvider,
        model: String,
        apiKey: String,
    ): AiNutrition? = aiApi.verifyNutrition(name, candidate, instructions, provider, model, apiKey)
    suspend fun setFoodFavourite(id: Long, favourite: Boolean) = foodDao.setFavourite(id, favourite)
    suspend fun deleteFood(id: Long) = foodDao.delete(id)

    /**
     * Delete a single custom food the user added. Two modes:
     *  - [cascadeLogs] = true → hard-delete the food row. FK CASCADE on
     *    `food_log.food_id` wipes every historical log that referenced it.
     *  - [cascadeLogs] = false → soft-delete (set is_hidden = 1). The food no
     *    longer surfaces in pick lists, but historical logs still resolve to it
     *    so the diary keeps rendering correctly.
     */
    suspend fun deleteCustomFood(id: Long, cascadeLogs: Boolean) {
        if (cascadeLogs) {
            foodDao.delete(id)
        } else {
            foodDao.hide(id)
        }
    }

    /** Bulk version of [deleteCustomFood] — affects every `is_custom = 1` row. */
    suspend fun deleteAllCustomFoods(cascadeLogs: Boolean) {
        if (cascadeLogs) {
            foodDao.deleteAllCustom()
        } else {
            foodDao.hideAllCustom()
        }
    }

    /** Custom foods still in the picker. */
    fun observeCustomVisibleFoods(): Flow<List<FoodEntity>> = foodDao.observeCustomVisible()

    /** Custom foods the user soft-deleted (kept only so old logs render). */
    fun observeCustomHiddenFoods(): Flow<List<FoodEntity>> = foodDao.observeCustomHidden()

    /** Bring a soft-deleted custom food back into the picker. */
    suspend fun restoreCustomFood(id: Long) = foodDao.unhide(id)

    /**
     * Online search for foods missing from the local database. Fans out in
     * parallel to:
     *   - OpenFoodFacts        — packaged products + some generic items
     *   - USDA FoodData Central — raw ingredients + branded products
     * Results are merged, de-duplicated by `name|brand`, and returned.
     * Nothing is persisted until the user picks a result via [saveOnlineResult].
     */
    suspend fun searchOnline(query: String, perSourceLimit: Int = 12): List<OnlineSearchResult> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyList()
        val offDeferred = async {
            runCatching { api.searchByName(query, perSourceLimit) }.getOrDefault(emptyList())
                .map { OnlineSearchResult(it, OnlineSource.OpenFoodFacts) }
        }
        val usdaDeferred = async {
            runCatching { usdaApi.searchByName(query, perSourceLimit) }.getOrDefault(emptyList())
                .map { OnlineSearchResult(it, OnlineSource.USDA) }
        }
        val combined = listOf(offDeferred, usdaDeferred).awaitAll().flatten()
        val seenKeys = HashSet<String>()
        combined.filter { result ->
            val key = (result.food.name + "|" + (result.food.brand ?: "")).lowercase()
            seenKeys.add(key)
        }
    }

    /**
     * Persist an online-search result so it appears in subsequent local searches,
     * favourites, etc. Returns the saved row with its assigned [id]. If a row with
     * the same barcode already exists we return that instead of inserting a duplicate.
     */
    suspend fun saveOnlineResult(food: FoodEntity): FoodEntity {
        food.barcode?.let { code ->
            foodDao.findByBarcode(code)?.let { return it }
        }
        val id = foodDao.upsert(food.copy(id = 0))
        return food.copy(id = id)
    }

    /**
     * Always-online barcode resolver. Nutrition info on packaged products
     * does change over time, so we re-fetch on every scan rather than
     * trusting whatever stale row may sit in the local cache.
     *
     * Sources are fanned out in parallel and reconciled in priority order:
     *   1. OpenFoodFacts — community-maintained, widest coverage of EAN/UPC
     *      (and the strongest source for Indian packaged products)
     *   2. USDA FoodData Central — authoritative US-branded products
     *
     * If every source misses, we fall back to whatever the local DB has
     * (so an earlier scan that did resolve still works offline). Returning
     * `food = null` from this method means: no source recognised the code,
     * the user should be offered manual entry.
     */
    suspend fun fetchByBarcode(barcode: String): BarcodeLookup = coroutineScope {
        if (barcode.isBlank()) return@coroutineScope BarcodeLookup()

        val offDeferred = async { runCatching { api.lookupBarcode(barcode) }.getOrNull() }
        val usdaDeferred = async { runCatching { usdaApi.lookupBarcode(barcode) }.getOrNull() }

        listOf(offDeferred, usdaDeferred).awaitAll()
        val off = offDeferred.await()
        val usda = usdaDeferred.await()

        val (product, source) = when {
            off != null -> off to BarcodeSource.OpenFoodFacts
            usda != null -> usda to BarcodeSource.USDA
            else -> null to null
        }
        if (product != null) {
            return@coroutineScope BarcodeLookup(product = product, source = source)
        }
        // All online sources missed — surface the cached row if we have one,
        // tagged as Cached so the UI can warn the user it might be stale.
        val cached = foodDao.findByBarcode(barcode)
        BarcodeLookup(cached = cached, source = cached?.let { BarcodeSource.Cached })
    }

    /**
     * Persist a barcode product the user has confirmed — they've picked the basis
     * (per-100g vs per-serving) on the resolved panel, and we normalize to per-100g
     * here for uniform downstream logging math.
     *
     * Marked `is_custom = 1` so barcode-scanned foods live alongside hand-entered
     * customs (visible in the Custom Foods list, deletable in bulk). Upserts by
     * barcode so future offline browses still find it, and so the freshest values
     * overwrite any earlier cached row.
     *
     * [servingGrams] / [servingLabel] override the source-reported serving so the
     * user can edit it on the resolve panel before saving; pass null to keep the
     * scan defaults.
     */
    suspend fun commitBarcodeProduct(
        product: BarcodeProduct,
        basis: NutritionBasis,
        servingGrams: Double? = null,
        servingLabel: String? = null,
    ): FoodEntity {
        val per100g = product.toPerHundredG(basis)
        val existing = foodDao.findByBarcode(product.barcode)
        val finalServingGrams = servingGrams ?: product.servingGrams
        val finalServingLabel = servingLabel ?: product.servingLabel
        val row = FoodEntity.fromNutrientsPer100g(
            name = product.name,
            brand = product.brand,
            barcode = product.barcode,
            category = product.category,
            n = per100g,
            servingGrams = finalServingGrams,
            servingLabel = finalServingLabel,
            isCustom = true,
            isFavourite = existing?.isFavourite ?: false,
        ).copy(id = existing?.id ?: 0)
        val id = foodDao.upsert(row)
        return row.copy(id = id)
    }

    /** Returns true if the seed was just inserted (i.e. table was empty). */
    suspend fun seedIfEmpty(): Boolean {
        if (foodDao.count() != 0) return false
        foodDao.upsertAll(IndianFoodSeed.ALL)
        return true
    }

    /**
     * Update existing seeded rows in place when [IndianFoodSeed.SEED_VERSION] bumps.
     *
     * For each food in the current seed: if a non-custom row with the same name exists,
     * we copy the new nutrient values over while preserving the user-owned fields
     * (id, isFavourite, custom servingVariants if any). Foods missing from the DB
     * are inserted. The user's food logs, weights, and custom additions are untouched.
     */
    suspend fun refreshSeedIfStale(storedVersion: Int): Boolean {
        if (storedVersion >= IndianFoodSeed.SEED_VERSION) return false
        // One read and one batched write, not two queries per seeded food. This
        // runs on the first launch after every SEED_VERSION bump, and looking
        // ~166 foods up one at a time meant ~332 separate transactions before
        // the user's first frame.
        //
        // The map is folded by hand rather than with associateBy because
        // associateBy keeps the LAST row for a duplicate name while
        // findSeededByName's LIMIT 1 took the first. Locale.ROOT because
        // SQLite's COLLATE NOCASE is ASCII-only folding — under a Turkish
        // default locale, lowercase('I') is 'ı' and the row would be missed.
        //
        // getAllSeeded, NOT getVisibleSeeded: this read must see hidden rows.
        // The refresh needs different semantics from the findSeededByName it
        // replaced — that lookup answers "which food should I show for this
        // name", where a hidden row rightly doesn't count, while this one asks
        // "does a row for this seed entry already exist", where it does. Read
        // through the visible-only filter, a hidden food looks absent, falls
        // to `return@map fresh`, and is inserted again as a brand-new visible
        // row with a new id and uuid: @Upsert finds no primary-key or
        // client_uuid conflict to update against. Hiding a seeded food would
        // therefore un-hide it as a duplicate on the next SEED_VERSION bump.
        val existingByName = HashMap<String, FoodEntity>()
        for (row in foodDao.getAllSeeded()) {
            existingByName.putIfAbsent(row.name.lowercase(Locale.ROOT), row)
        }

        val updated = IndianFoodSeed.ALL.map { fresh ->
            val existing = existingByName[fresh.name.lowercase(Locale.ROOT)]
                ?: return@map fresh
            fresh.copy(
                id = existing.id,
                // Carried explicitly: `fresh` defaults it to false, so without
                // this the wider read above would un-hide every hidden food
                // instead of duplicating it — the same bug wearing a different
                // hat. Hiding is the user's decision about a seeded food, and
                // a nutrient correction is not a reason to overturn it.
                isHidden = existing.isHidden,
                // Preserve the stable cross-install uuid; a fresh seed list
                // generates a new random one, which would otherwise break
                // backup/restore merge keyed on client_uuid.
                clientUuid = existing.clientUuid,
                isFavourite = existing.isFavourite,
                // Keep the user's custom variants if they edited them; otherwise
                // adopt the fresh seed's variants so renamed labels, new gram
                // refs, and new size options (e.g. banana Small/Medium/Large)
                // propagate to already-installed apps. We treat the row as
                // "not customized" when its variants are still exactly the
                // auto-generated defaults for its serving.
                servingVariants = when {
                    existing.servingVariants.isEmpty() -> fresh.servingVariants
                    existing.servingVariants == ServingVariant.defaultVariantsFor(
                        existing.servingGrams, existing.servingLabel,
                    ) -> fresh.servingVariants
                    // Also adopt fresh variants when the existing ones are still
                    // exactly the auto-derived Small/Medium/Large presets — those
                    // aren't user customization either, so a corrected seed (e.g.
                    // real egg sizes replacing the ×0.7/×1.4 scale) must propagate.
                    existing.servingVariants == IndianFoodSeed.sizeVariantsFor(
                        existing.servingGrams, existing.servingLabel,
                    ) -> fresh.servingVariants
                    else -> existing.servingVariants
                },
            )
        }
        foodDao.upsertAll(updated)
        return true
    }

    // ----- Food log -----
    fun observeLogForDay(epochDay: Long): Flow<List<FoodLogWithFood>> =
        foodLogDao.observeForDay(epochDay)

    fun observeLogForRange(start: Long, end: Long): Flow<List<FoodLogWithFood>> =
        foodLogDao.observeForRange(start, end)

    fun observeRecentFoods(limit: Int = 12): Flow<List<FoodEntity>> =
        foodLogDao.observeRecentFoods(limit)

    /** Direct (non-Flow) reads — used by widget rendering so we get the
     *  latest state immediately after a write, dodging the Room
     *  InvalidationTracker propagation lag. */
    suspend fun getLogForDay(epochDay: Long): List<FoodLogWithFood> =
        foodLogDao.getForDay(epochDay)
    suspend fun getRecentFoods(limit: Int): List<FoodEntity> =
        foodLogDao.getRecentFoods(limit)

    fun observeFrequentFoods(days: Int = 30, limit: Int = 12): Flow<List<FoodEntity>> =
        foodLogDao.observeFrequentFoods(DateUtil.todayEpochDay() - days, limit)

    suspend fun addFoodLog(entry: FoodLogEntity): Long =
        foodLogDao.insert(entry).also { onWidgetDataChanged() }
    suspend fun deleteFoodLog(id: Long) {
        foodLogDao.delete(id)
        onWidgetDataChanged()
    }

    // ----- Water -----
    fun observeWaterForDay(epochDay: Long): Flow<List<WaterLogEntity>> =
        waterLogDao.observeForDay(epochDay)
    fun observeWaterTotalForDay(epochDay: Long): Flow<Int> =
        waterLogDao.observeTotalForDay(epochDay)
    fun observeWaterTotalsForRange(start: Long, end: Long): Flow<List<DailyWaterTotal>> =
        waterLogDao.observeTotalsForRange(start, end)
    suspend fun addWater(entry: WaterLogEntity): Long = waterLogDao.insert(entry)
    suspend fun deleteWater(id: Long) = waterLogDao.delete(id)

    // ----- Weight -----
    fun observeRecentWeights(limit: Int = 60): Flow<List<WeightLogEntity>> =
        weightLogDao.observeRecent(limit)
    fun observeWeightsForRange(start: Long, end: Long): Flow<List<WeightLogEntity>> =
        weightLogDao.observeForRange(start, end)
    fun observeLatestWeight(): Flow<WeightLogEntity?> = weightLogDao.observeLatest()
    suspend fun upsertWeight(entry: WeightLogEntity): Long = weightLogDao.upsert(entry)
    suspend fun deleteWeight(id: Long) = weightLogDao.delete(id)
}
