/**
 * WordDefinitions.kt
 * This file is part of ASLRecorder, licensed under the MIT license.
 *
 * Copyright (c) 2021 Sahir Shahryar <contact@sahirshahryar.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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