package edu.gatech.ccg.aslrecorder.splash

import edu.gatech.ccg.aslrecorder.R

// TODO: Internationalization?
enum class WordDefinitions(val resourceId: Int, val title: String, val desc: String) {
    ANIMALS(R.array.animals, "Animals", "Signs for animals"),
    FOOD(R.array.food, "Food", "Types of food"),
    HOUSEHOLD(R.array.household, "Household", "Household items and furniture"),
    PEOPLE(R.array.people, "People", "Signs for relatives and other people"),
    BODY_PARTS(R.array.body, "Body Parts", "Signs for various parts of the human body"),
    OUTDOORS(R.array.outdoors, "Outdoors", "Places and objects found outside"),
    ROUTINES(R.array.routines, "Routines", "Signs related to routines"),
    ACTIONS(R.array.actions, "Actions", "Signs for common verbs"),
    ADJECTIVES(R.array.adjectives, "Adjectives", "Signs for common adjectives"),
    HELPER_WORDS(R.array.helper_words, "Helper words",
        "Prepositions, modifier words, etc."),
    PRONOUNS(R.array.pronouns, "Pronouns", "Signs for pronouns")
}