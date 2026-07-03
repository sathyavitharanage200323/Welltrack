# Calendar View Overlap Fix

## Issue
The calendar view was overlapping with the "List view" and "Calendar View" toggle buttons, making them inaccessible.

## Root Cause
The calendar view was being dynamically added to the parent container without proper layout constraints, causing it to overlap with other UI elements.

## Solution
Added a dedicated `FrameLayout` container for the calendar view with proper constraints:

### Changes Made

1. **`fragment_mood_journal.xml`**
   - Added `content_container` FrameLayout
   - Moved `mood_entries_recycler_view` inside container
   - Added `calendar_container` FrameLayout for calendar view
   - Updated empty state constraints

2. **`MoodJournalFragment.kt`**
   - Updated `showCalendarView()` to use dedicated calendar container
   - Updated `showListView()` to hide calendar container
   - Proper visibility management for all views

## Layout Structure (Fixed)

```
ConstraintLayout
├── header_layout (Mood Journal + Statistics button)
├── view_toggle_layout (List View / Calendar View buttons)
├── content_container (FrameLayout)
│   ├── mood_entries_recycler_view (List view)
│   └── calendar_container (Calendar view)
├── empty_state (No entries message)
└── add_mood_button (FAB)
```

## How It Works Now

### List View
- `calendar_container` → GONE
- `mood_entries_recycler_view` → VISIBLE
- Toggle buttons → Always visible and accessible

### Calendar View
- `calendar_container` → VISIBLE
- `mood_entries_recycler_view` → GONE
- Toggle buttons → Always visible and accessible

## Testing

1. ✅ Open Mood Journal
2. ✅ Tap "Calendar View" button
3. ✅ Calendar displays without overlapping buttons
4. ✅ Toggle buttons remain accessible
5. ✅ Tap "List View" button
6. ✅ List displays correctly
7. ✅ Switch between views multiple times

## Result

✅ Calendar view no longer overlaps with toggle buttons  
✅ Both buttons are always accessible  
✅ Smooth transitions between list and calendar views  
✅ Proper layout hierarchy maintained  

The calendar view now displays correctly within its dedicated container!
