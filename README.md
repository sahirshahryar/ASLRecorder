# ASLRecorder (WIP)

Android app for Deaf and hard-of-hearing (DHH) users to record training videos for
sign language recognition models.


## Progress
- [x] Show camera feed
- [x] Save video recordings
- [x] Use round button instead of rectangular one
- [x] Show which word the user should sign at the top of the interface
  - [x] Automatically label recordings by the word shown at the top of the UI
- [ ] Actual list of words!
- [ ] Dynamic device compatibility (query device capabilities instead of assuming them)
  - [ ] Fetch camera list
  - [ ] Set video quality to selected camera's resolution
- [ ] Check permissions and ask if not granted
- [ ] Test and tweak haptic feedback
- [ ] UI Improvements
  - [ ] Home page, tutorial?
  - [ ] **Option to delete a botched recording** (better for us too!)
- [ ] Persistence? (Store which signs users have already recorded so they don't need to re-record)
- [ ] Multitasking support (current version may crash on exit/reopen)
- [ ] Easy access to video files (upload? direct file browser?)



## Notes
* To run this app in the Android emulator, you may need to explicitly enable the
webcam. In Android Studio, go to `Tools` > `AVD Manager`, click the pencil
(edit) button for the emulator of your choice, press `Show Advanced Settings`,
and choose your webcam (likely `Webcam0`) under `Camera` > `Front`. Press
`Finish` to save.

* If you encounter issues where the entire Android emulator crashes when trying
to run the app, go to the same window as above, press `Show Advanced Settings`,
and set `Emulated Performance` > `Graphics` to `Software`.
