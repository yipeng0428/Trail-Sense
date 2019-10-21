package com.kylecorry.survival_aid.blueprints

/**
 * A blueprint for creating a resource
 * @param resource the resource that the blueprint will create
 * @param ingredients the ingredients to produce this resource - pairs of resources with quantities
 * @param steps the steps to create the resource
 */
data class Blueprint(val resource: Resource,
                     val category: BlueprintCategory,
                     val ingredients: List<Pair<Resource, Quantity>>,
                     val steps: List<String> = emptyList())


enum class BlueprintCategory {
    WATER,
    FOOD,
    ELECTRICITY,
    OTHER
}