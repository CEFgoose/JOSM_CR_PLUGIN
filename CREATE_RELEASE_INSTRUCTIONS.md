# Instructions to Create GitHub Release

Follow these steps to create the release on GitHub:

## 1. Navigate to the Releases Page
Go to: https://github.com/CEFgoose/JOSM_CR_PLUGIN/releases

## 2. Create New Release
Click the "Draft a new release" button

## 3. Configure Release Settings
- **Choose a tag**: Select `v1.0.0` (already pushed)
- **Release title**: `JOSM Conditional Restrictions Plugin v1.0.0`
- **Description**: Copy the contents from `RELEASE_NOTES_v1.0.0.md`

## 4. Attach the JAR File
- Click "Attach binaries by dropping them here or selecting them"
- Upload the `ConditionalRestrictions.jar` file from this directory

## 5. Publish Release
- Make sure "Set as the latest release" is checked
- Click "Publish release"

## Alternative: Command Line (if gh auth works)
```bash
gh auth login
gh release create v1.0.0 ConditionalRestrictions.jar \
  --title "JOSM Conditional Restrictions Plugin v1.0.0" \
  --notes-file RELEASE_NOTES_v1.0.0.md
```

## After Creating the Release
The download URL will be:
https://github.com/CEFgoose/JOSM_CR_PLUGIN/releases/download/v1.0.0/ConditionalRestrictions.jar

Update the README.md to include this download link.