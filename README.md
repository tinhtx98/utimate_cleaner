UltimateCleaner

 1. Basic Project Information:
 
 [x] applicationId: com.mars.ultimatecleaner
 
 [x] Primary Language: Kotlin

 [x] UI Framework: Jetpack Compose

 [x] Architecture: MVVM (Model-View-ViewModel)

 [x] Dependency Injection: Hilt
 
 [x] minSdk: 26
 
 [x] targetSdk = 36

 2. Memory Cleaning Features 

 [x] Clean Junk Files (cache, temporary files, residual files) 

 Details: (e.g., Do you want to categorize junk by application? Do you want to display details of each junk file?) 

 UI/UX Notes: (e.g., Scan progress bar, color display for junk size.) 

 [x] Clean Obsolete APK Files 

 Details: (e.g., Do you want to display APK versions? Do you want to differentiate between installed and uninstalled APKs?) 

 UI/UX Notes: (e.g., APK icon, clear delete button.) 

 [x] Clean Empty Folders 

 Details: (e.g., Do you want to display the path of empty folders?) 

 UI/UX Notes: (e.g., Simple list, "delete all" button.) 

 [x] Clean Large Files 

 Details: (e.g., Do you want to allow users to set a large file size threshold? Do you want to categorize large files by type (video, document)?) 

 UI/UX Notes: (e.g., File size distribution chart, sortable list.) 

 3. File Management Features 

 [x] Browse Files by Category (Photos, Videos, Documents, Audio, etc.) 

 Details: (e.g., Do you want to support specific file formats? Do you want to display the number of files in each category?) 

 UI/UX Notes: (e.g., Tabs or category cards, illustrative icons for each file type.) 

 [x] File Search 

 Details: (e.g., Do you want to support searching by name, file type, or modification date?) 

 UI/UX Notes: (e.g., Search bar always visible, real-time updated search results.) 

 [x] Move/Copy/Delete Files/Folders 

 Details: (e.g., Do you want to support multi-selection of files? Do you want a confirmation dialog before deletion?) 

 UI/UX Notes: (e.g., Clear action buttons (Move, Copy, Delete), contextual toolbar when files are selected.) 

 [x] Rename Files/Folders 

 Details: (e.g., Do you want new name suggestions?) 

 UI/UX Notes: (e.g., Simple new name input dialog.) 

 [x] Create New Folders 

 Details: (e.g., Do you want to allow creating subfolders within the current folder?) 

 UI/UX Notes: (e.g., Easily visible "Create Folder" button.) 

 4. Optimization Features 

 [x] Find and Delete Duplicate Photos/Videos 

 Details: (e.g., Do you want to compare by content (hash) or just by name/size? Do you want to display original and duplicate copies for user selection?) 

 UI/UX Notes: (e.g., Photo comparison interface, display of potential space savings.) 

 [x] Find and Delete Old/Unused Screenshots 

 Details: (e.g., Do you want to define "old" by number of days? Do you want to suggest based on frequency of opening?) 

 UI/UX Notes: (e.g., List with photo previews, quick delete button.) 

 [x] Compress Photos/Videos (lossless or minimal loss) 

 Details: (e.g., Do you want to support different compression levels? Do you want to display file size before and after compression?) 

 UI/UX Notes: (e.g., Compression level slider, display of compression results.) 

 [x] App Management (uninstall, view app info) 

 Details: (e.g., Do you want to display system apps? Do you want to display the cache size of each app?) 

 UI/UX Notes: (e.g., App list with icon and size, clear "Uninstall" button.) 

 5. AI/Smart Features 

 [x] Smart Cleaning Suggestions based on Usage Habits 

 Details: (e.g., Suggestions based on which apps generate the most junk? Suggestions based on which files are least accessed?) 

 UI/UX Notes: (e.g., Suggestion notifications on the home screen, explanation of suggestion reasons.) 

 [x] Blurry/Bad Photo Recognition and Deletion Suggestion 

 Details: (e.g., Do you want to use an algorithm to detect blurriness/sharpness? Do you want to allow users to adjust the detection threshold?) 

 UI/UX Notes: (e.g., Display blurry photos with warnings, photo preview.) 

 [x] Automatic Scheduled Cleaning (user configurable) 

 Details: (e.g., Frequency options (daily, weekly, monthly)? Types of junk to be cleaned automatically?) 

 UI/UX Notes: (e.g., Clear schedule settings interface, on/off toggle.) 

 [x] Send Periodic Notifications to Remind Users to Clean Memory 

 Details: (e.g., Notification frequency? Customizable notification content?) 

 UI/UX Notes: (e.g., Notification settings toggle, example notification content.) 

 6. General UI/UX Notes (Applies to all screens): 

 Design Style: (e.g., Vibrant, colorful, modern, minimalist.) 

 Overall Layout: (e.g., Easy to use, intuitive, with intuitive charts/graphs.) 

 General Elements: (e.g., Clear icons, smooth animations, haptic feedback.) 

 7. Other Special Notes or Requirements: 

 (Anything else you'd like to add that hasn't been covered, e.g., specific performance requirements, or unique ideas.) 

 Code Structure: All screens should be built with separate Composables and their state managed by dedicated ViewModels.
 
 8. Add all necessary files including
 
 - build.gradle.kts module level project, app
 
 - libs.versions.toml
 
 - settings.gradle.kts
 
 - gradle-wrapper.properties
 
 - AndroidManifest.xml with permissions to use app
