<h1 align=center>更新日志</h1>

```markdown
## [0.3.8] - 2018-08-27
- 0.3.7(818) -> 0.3.8(847)

### Finished
- Switch between GPS and Network provider automatically
- Notification for background service when location disabled
```

```markdown
## [0.3.7] - 2018-08-25
- Adapted for Android O
- 0.3.6(783) -> 0.3.7(818)

### Fixed
- In Android O, app crashes when open the Background Limitation Explanation
- In Android O, notification doesn't show when background service activated
- In Android O, app icon display incorrectly
```

```markdown
## [0.3.6] - 2018-08-25
- 0.3.5(776) -> 0.3.6(783)

### Finished
- Set random seed which used to generate check points
- Display index when touch markers during sequential missions
- Push notification when check in background
```

```markdown
## [0.3.5] - 2018-08-25
- 0.3.4(755) -> 0.3.5(776)

### Finished
- Background description in Preference
- Warn user when try to exit during mission (Press back)

### Changed
- UI improved
```

```markdown
## [0.3.4] - 2018-08-25
- 0.3.3(714) -> 0.3.4(755)

### Finished
- Background Mission Service in Foreground

### Fixed
- Dashboard doesn't show in some conditions
```

```markdown
## [0.3.3] - 2018-08-25
- 0.3.2(662) -> 0.3.3(714)

### Finished
- Background Mission Service (Not fully functional)
```

```markdown
## [0.3.2] - 2018-08-24
- 0.3.1(640) -> 0.3.2(662)

### Finished
- Display track when mission stopped

### Fixed
- Location may be fixed twice when app launched
```

```markdown
## [0.3.1] - 2018-08-24
- 0.3(624) -> 0.3.1(640)

### Changed
- Improved CORC method
```

```markdown
## [0.3] - 2018-08-23
- 0.2.3(611) -> 0.3(624)

### Finished
- Track Record: Algorithm (CORC) and part of functions

### Changed
- Waypoint -> GeoPoint, and CheckPoint & TrackPoint inherited from it
```

```markdown
## [0.2.3] - 2018-08-23
- 0.2.2(580) -> 0.2.3(611)

### Finished
- Sequential mission

### Fixed
- App crashes when launch with location provider unavailable
- In SetupActivity, contents don't refresh when the value is reset by code
```

```markdown
## [0.2.2] - 2018-08-22
- 0.2.1(561) -> 0.2.2(580)

### Finished
- Checked dialog and all checked dialog
- DialogKit to display dialogs in a simple way
```

```markdown
## [0.2.1] - 2018-08-22
- 0.2(549) -> 0.2.1(561)

### Finished
- Real Time Dashboard
```

```markdown
## [0.2] - 2018-08-21
- 0.1.14(463) -> 0.2(549)

### Finished
- Mission time including timer system in MissionManager and UI in Dashboard

### Added
- FAB for Preference
- Pause state for mission
- Animation for entering & exiting SetupActivity

### Removed
- Toolbar and MainMenu methods in MainActivity
```

```markdown
## [0.1.14] - 2018-08-21
- 0.1.13(436) -> 0.1.14(463)

### Finished
- Dashboard fab and dialog

### Removed
- Menu item StartStop, replaced by a button in Dashboard
```

```markdown
## [0.1.13] - 2018-08-20
- 0.1.12(403) -> 0.1.13(436)

### Finished
- Method: MapKit.resetZoomAndCenter()
```

```markdown
## [0.1.12] - 2018-08-20
- 0.1.11(342) -> 0.1.12(403)

### Finished
- Progress Bar

### Fixed
- Lots of issues caused by loading mission data in wrong time
- Menu not totally disabled when starting
```

```markdown
## [0.1.11] - 2018-08-20
- 0.1.10(332) -> 0.1.11(342)

### Finished
- Reset Camera FAB

### Changed
- Use callback to handle tasks after map initialized
```

```markdown
## [0.1.10] - 2018-08-19
- 0.1.9(300) -> 0.1.10(332)

### Finished
- MapKit.MarkerType with properties, methods and resources
- Methods
  - MapKit: addMarkerAt(), changeMarkerIconAt()
  - MissionManager: reach()
  - MainActivity: onChecked(), onFinishedAll()

### Fixed
- Algorithm for fixCoordinate()
```

```markdown
## [0.1.9] - 2018-08-19
- 0.1.8(244) -> 0.1.9(300)

### Finished
- Methods
  - MainActivity: onPrepareOptionsMenu()
  - MissionManager: pause(), resume(), stop()
  - MapKit: clearMarkers()

### Added
- Save makers when map is not initialized and add them once the map initialized
```

```markdown
## [0.1.8] - 2018-08-19
- 0.1.7(237) -> 0.1.8(244)

### Finished
- Methods: Value Checker for SetupFragment

### Added
- Zoom range for map

### Changed
- Use kotlin.math in LocationKit.fixCoordinate()
```

```markdown
## [0.1.7] - 2018-08-18
- 0.1.6(217) -> 0.1.7(237)

### Finished
- Class: LocationKit
- Methods: MissionManager.start(), MissionManager.generateWaypointList()

### Changed
- Constructor of Waypoint
```

```markdown
## [0.1.6] - 2018-08-18
- 0.1.5(183) -> 0.1.6(217)

### Finished
- Package map functions in class MapKit

### Changed
- Coordinate converter improved
```

```markdown
## [0.1.5] - 2018-08-18
- 0.1.4(163) -> 0.1.5(183)

### Finished
- Class: LocationKit

### Fixed
- Algorithm of coordinate converter
```

```markdown
## [0.1.4] - 2018-08-17
- 0.1.3(86) -> 0.1.4(163)

### Finished
- Class: TrumeKit and Waypoint
- Frame: LocationKit and MissionManager

### Fixed
- MineMap SDK always crashes
```

```markdown
## [0.1.3] - 2018-08-17
- 0.1.2(62) -> 0.1.3(86)

### Finished
- App frame including passing message between activities

### Changed
- App icon adjusted

### Notice
- Ready to add MineMap SDK but something's wrong with it
```

```markdown
## [0.1.2] - 2018-08-16
- 0.1.1(34) -> 0.1.2(62)

### Finished
- App frame including setup activity
```

```markdown
## [0.1.1] - 2018-08-16
- 0.1(10) -> 0.1.1(34)

### Finished
- App Icon
- App frame including preference

### Changed
- Package name: lab.zero_one -> labs.zero_one
```

```markdown
## [0.1] - 2018-08-15
- 0.1(10)
- Initial version
```
