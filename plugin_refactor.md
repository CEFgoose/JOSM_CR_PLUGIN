# JOSM Conditional Restrictions Plugin - Modernization Plan

## Phase 1: Error Analysis & Categorization

### Java 8 Compatibility Issues
1. **`Map.of()` usage** (TimeUtils.java:71)
   - Replace with traditional HashMap initialization
   - Affects MONTH_ABBREVIATIONS map creation

### JOSM API Compatibility Issues  
2. **TestError API changes** (ConditionalValidator.java)
   - `setDescription()` method doesn't exist
   - `fixError()` return type changed

3. **Dialog Interface changes** (ConditionalRestrictionsDialog.java:194)
   - `showDialog()` return type incompatible with IExtendedDialog
   - Interface signature changed

4. **MapView API changes** (ConditionalRestrictionsRenderer.java:63)
   - `getZoomLevel()` method removed/renamed
   - `invalidate()` visibility changed (private vs public)

5. **ImageProvider API changes** (ConditionalRestrictionsPreferences.java:84)
   - Return type incompatible with JTabbedPane
   - Tab icon setup method changed

## Phase 2: Java 8 Compatibility Fixes

### Target Files:
- `utils/TimeUtils.java` - Replace Map.of() with HashMap
- Update any other Java 9+ features to Java 8 equivalents

## Phase 3: JOSM API Modernization

### Priority Order:
1. **Core Plugin Class** - ConditionalRestrictionsPlugin.java
2. **Validator Integration** - ConditionalValidator.java  
3. **UI Components** - ConditionalRestrictionsDialog.java
4. **Map Rendering** - ConditionalRestrictionsRenderer.java
5. **Preferences** - ConditionalRestrictionsPreferences.java

### Required Research:
- Current JOSM TestError constructor/methods
- Modern IExtendedDialog interface requirements
- MapView zoom level access patterns
- ImageProvider tab icon setup
- Current Command interface for validator fixes

## Phase 4: GraphView Integration Update

### Tasks:
- Download actual GraphView plugin JAR
- Verify integration points still exist in current GraphView
- Update integration class if needed

## Phase 5: Testing & Validation

### Build Verification:
- Clean compilation without errors
- JAR creation successful
- Plugin loads in JOSM without crashes

### Functional Testing:
- Basic plugin activation
- Conditional restriction parsing
- UI dialog functionality
- Map visualization rendering

## Estimated Effort
- **Java 8 fixes**: 30 minutes
- **JOSM API updates**: 2-3 hours  
- **Testing & validation**: 1 hour
- **Total**: 3-4 hours

## Current Status
Plugin development complete but needs API compatibility updates for current JOSM version. All core architecture and business logic implemented correctly.