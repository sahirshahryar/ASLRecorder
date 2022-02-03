/**
 * Utilities.kt
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
package edu.gatech.ccg.aslrecorder

import kotlin.random.Random

/**
 * Selects `count` elements from `list` at random, using the designated seed if given,
 * or a random seed otherwise.
 */
fun <T> randomChoice(list: List<T>, count: Int, seed: Long? = null): ArrayList<T> {
    if (count == 0) {
        return ArrayList()
    }

    /**
     * Because Kotlin pointlessly restricts function parameters to being constants,
     * we need to make this duplicate variable
     */
    var seed2 = seed
    if (seed2 == null) {
        seed2 = Random.nextLong()
    }

    // Initialize Random object and resulting ArrayList.
    val rand = Random(seed2)
    var result = ArrayList<T>()

    if (count == 1) {
        result.add(list[rand.nextInt(list.size)])
        return result
    }

    /**
     * pickedSet: a HashSet of array indices which have been already selected. Used for
     *            determining, in O(1) time, which elements have already been chosen and
     *            should not be re-chosen for the result.
     * pickedList: an ArrayList of indices which have already been selected. This is used
     *             after random selection is complete, in order to map the selected indices
     *             to their matching elements in the original list.
     */
    val pickedSet = HashSet<Int>()
    val pickedList = ArrayList<Int>()

    /**
     * This algorithm trades space efficiency for time efficiency in cases where the user
     * has asked for a random selection of MOST of the elements in the original list (for example,
     * choosing 9 elements out of a list of 10 elements). Once you get to a point where most
     * indices have already been selected, it takes an increasing number of random selections
     * to pick an index that has not already been included in the random choice. When choosing
     * the final element in the above example, eight of the ten indices will already have been
     * chosen, meaning the last choice requires (on average) 5 attempts to choose a valid
     * index. This can get much, much worse when the size of the array gets larger.
     *
     * To stave off this issue, this algorithm creates a list of indices that have not yet been
     * picked ONLY once we cross a specific threshold. This prevents us from creating a comically
     * large list of indices that haven't been selected when it doesn't save us much time.
     * However, once we cross that threshold, taking the effort to create a list of indices that
     * haven't been picked yet, then choosing at random from that list, saves a lot of time.
     *
     * notYetPicked: an ArrayList of indices of the original list that have not yet been selected
     *               for the final result. The size of this list will be no greater than
     *               list.size * (1 - threshold).
     * selectingFromNYP: a boolean dictating whether we are selecting indices from notYetPicked
     *                   or still picking indices at random in the range [0, list.size).
     * threshold: the point at which selectingFromNYP should become true and notYetPicked should
     *            be constructed. Note that this value is compared to `i / list.size` (i.e., we
     *            are dividing the current number of selected indices versus the total size of
     *            the list). If threshold = 0.8, and we are selecting 3 items from a list of 10,
     *            then we will never cross the threshold and activate this function.
     */
    val notYetPicked = ArrayList<Int>()
    var selectingFromNYP = false
    val threshold = 0.8

    for (i in 1..count) {
        var index: Int

        // Late stages of the selection: pick directly from indices not yet selected
        if (selectingFromNYP) {
            val nypIndex = rand.nextInt(notYetPicked.size)
            index = notYetPicked[nypIndex]
            notYetPicked.removeAt(nypIndex)
        }

        // Early stages of the selection: select indices at random
        else {
            do {
                index = rand.nextInt(list.size)
            } while (pickedSet.contains(index))
        }

        pickedSet.add(index)
        pickedList.add(index)

        // Determine whether we need to set selectingFromNYP to true
        // (have we crossed the threshold?)
        if (i < count && !selectingFromNYP) {
            val ratio = i.toFloat() / list.size
            if (ratio > threshold) {
                selectingFromNYP = true
                for (j in list.indices) {
                    if (!pickedSet.contains(j)) {
                        notYetPicked.add(j)
                    }
                }
            }
        }
    }

    for (index in pickedList) {
        result.add(list[index])
    }

    return result
}