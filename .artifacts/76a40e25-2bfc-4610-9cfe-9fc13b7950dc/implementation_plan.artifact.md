# Implementation Plan - Vehicle Profile and Adaptive UI Shell

This plan covers the implementation of the Vehicle Profile screen and the adaptive UI shell for the Glovebox app.

## Proposed Changes

### Data Layer

#### [MODIFY] [VehicleDao.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/dao/VehicleDao.kt)
- No changes needed, already has necessary methods.

### UI Layer - ViewModels

#### [NEW] [VehicleViewModel.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/ui/viewmodels/VehicleViewModel.kt)
- Create a ViewModel to manage vehicle data.
- Fetch the first vehicle (as there's likely only one for now) or handle multiple if needed.
- Provide methods to save/update vehicle details.

### UI Layer - Screens

#### [MODIFY] [VehicleProfileScreen.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/ui/screens/VehicleProfileScreen.kt)
- Implement UI fields: Year (Int), Make (String), Model (String), Odometer (Int).
- Connect to `VehicleViewModel`.
- Add a Save button.

### Navigation & Shell

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/MainActivity.kt)
- Integrate `NavigationSuiteScaffold` (or `adaptive-navigation3` wrapper) to provide an adaptive UI shell.
- Connect the shell to the Navigation 3 backstack.
- Add navigation items for Vehicle Profile, Service History, and Reminders.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors: `./gradlew assembleDebug`

### Manual Verification
- Verify the `docs/policy.html` was created correctly.
- Open the app and verify the adaptive navigation (Rail on wide screens, Bar on narrow screens).
- Fill in the Vehicle Profile and save. Verify data persists after app restart.
- Verify navigation between different screens works as expected.
