# Room Database and Navigation 3 Foundation Setup

Setup the Room database architecture and the basic Navigation 3 structure for the Glovebox app.

## Proposed Changes

### Room Database

#### [NEW] [Vehicle.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/entity/Vehicle.kt)
Define the `Vehicle` entity with fields: `id`, `make`, `model`, `year`, `odometer`.

#### [NEW] [ServiceRecord.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/entity/ServiceRecord.kt)
Define the `ServiceRecord` entity with fields: `id`, `vehicleId`, `date`, `mileage`, `serviceType`, `cost`, `notes`.

#### [NEW] [Reminder.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/entity/Reminder.kt)
Define the `Reminder` entity with fields: `id`, `vehicleId`, `description`, `targetMileage`, `targetDate`, `isCompleted`.

#### [NEW] [VehicleDao.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/dao/VehicleDao.kt)
Define CRUD operations for `Vehicle`.

#### [NEW] [ServiceRecordDao.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/dao/ServiceRecordDao.kt)
Define CRUD operations for `ServiceRecord`.

#### [NEW] [ReminderDao.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/dao/ReminderDao.kt)
Define CRUD operations for `Reminder`.

#### [NEW] [GloveboxDatabase.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/data/GloveboxDatabase.kt)
Define the main Room database class.

### Navigation 3 Foundation

#### [NEW] [GloveboxRoute.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/navigation/GloveboxRoute.kt)
Define `@Serializable` keys for all routes: `VehicleProfile`, `ServiceHistory`, `AddServiceLog`, `Reminders`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Nick/AndroidStudioProjects/Glovebox/app/src/main/java/com/eliteonetube/glovebox/MainActivity.kt)
Initialize `NavBackStack` and `NavDisplay` to set up the basic navigation structure.

## Verification Plan

### Automated Tests
- Run `./gradlew build` to ensure the Room compiler and KSP generate code correctly and the project compiles.

### Manual Verification
- Verify the navigation state-driven structure in `MainActivity.kt`.
