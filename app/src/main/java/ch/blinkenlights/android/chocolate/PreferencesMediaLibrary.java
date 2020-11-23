/*
 * Copyright (C) 2017-2018 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ch.blinkenlights.android.chocolate;

import ch.blinkenlights.android.medialibrary.MediaLibrary;
import ch.blinkenlights.android.medialibrary.LibraryObserver;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class PreferencesMediaLibrary extends Fragment implements View.OnClickListener
{
	/**
	 * The ugly timer which fires every 200ms
	 */
	private Timer mTimer;
	/**
	 * Our start button
	 */
	private View mStartButton;
	/**
	 * The cancel button
	 */
	private View mCancelButton;
	/**
	 * The edit-media-folders button
	 */
	private View mEditButton;
	/**
	 * The debug / progress text describing the scan status
	 */
	private TextView mProgressText;
	/**
	 * The progress bar
	 */
	private ProgressBar mProgressBar;
	/**
	 * The number of tracks on this device
	 */;
	private TextView mStatsTracks;
	/**
	 * The number of hours of music we have
	 */
	private TextView mStatsLibraryPlaytime;
	/**
	 * The total listening time
	 */
	private TextView mStatsListenPlaytime;
	/**
	 * A list of scanned media directories
	 */
	private TextView mMediaDirectories;
	/**
	 * Checkbox for full scan
	 */
	private CheckBox mFullScanCheck;
	/**
	 * Checkbox for drop
	 */
	private CheckBox mDropDbCheck;
	/**
	 * Checkbox for album group option
	 */
	private CheckBox mGroupAlbumsCheck;
	/**
	 * Checkbox for targreader flavor
	 */
	private CheckBox mForceBastpCheck;
	/**
	 * Set if we should start a full scan due to option changes
	 */
	private boolean mFullScanPending;
	/**
	 * Set if we are in the edit dialog
	 */
	private boolean mIsEditingDirectories;
	/**
	 * The original state of our media folders.
	 */
	private String mInitialMediaFolders;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.medialibrary_preferences, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mStartButton = (View)view.findViewById(R.id.start_button);
		mCancelButton = (View)view.findViewById(R.id.cancel_button);
		mEditButton = (View)view.findViewById(R.id.edit_button);
		mProgressText = (TextView)view.findViewById(R.id.media_stats_progress_text);
		mProgressBar = (ProgressBar)view.findViewById(R.id.media_stats_progress_bar);
		mStatsTracks = (TextView)view.findViewById(R.id.media_stats_tracks);
		mStatsLibraryPlaytime = (TextView)view.findViewById(R.id.media_stats_library_playtime);
		mStatsListenPlaytime = (TextView)view.findViewById(R.id.media_stats_listen_playtime);
		mMediaDirectories = (TextView)view.findViewById(R.id.media_directories);
		mFullScanCheck = (CheckBox)view.findViewById(R.id.media_scan_full);
		mDropDbCheck = (CheckBox)view.findViewById(R.id.media_scan_drop_db);
		mGroupAlbumsCheck = (CheckBox)view.findViewById(R.id.media_scan_group_albums);
		mForceBastpCheck = (CheckBox)view.findViewById(R.id.media_scan_force_bastp);
		View mCleanLibrary = view.findViewById(R.id.clean_library);
		View mRefreshFavorites = view.findViewById(R.id.favorites_button);

		// Bind onClickListener to some elements
		mStartButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);
		mEditButton.setOnClickListener(this);
		mGroupAlbumsCheck.setOnClickListener(this);
		mForceBastpCheck.setOnClickListener(this);
		mCleanLibrary.setOnClickListener(this);
		mRefreshFavorites.setOnClickListener(this);

		// Enable options menu.
		setHasOptionsMenu(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		mTimer = new Timer();
		// Yep: its as ugly as it seems: we are POLLING
		// the database.
		mTimer.scheduleAtFixedRate((new TimerTask() {
			@Override
			public void run() {
				getActivity().runOnUiThread(new Runnable(){
					public void run() {
						updateProgress();
					}
				});
			}}), 0, 200);

		// We got freshly created and user didn't have a chance
		// to edit anything: remember this state
		if (mInitialMediaFolders == null)
			mInitialMediaFolders = getMediaFoldersDescription();

		// Returned from edit dialog
		if (mIsEditingDirectories) {
			mIsEditingDirectories = false;
			// trigger a scan if user changed its preferences
			// in the edit dialog
			if (!mInitialMediaFolders.equals(getMediaFoldersDescription()))
				mFullScanPending = true;
		}

		updatePreferences(null);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		// User exited this view -> scan if needed
		if (mFullScanPending && !mIsEditingDirectories) {
			MediaLibrary.startLibraryScan(getActivity(), true, true);
			mInitialMediaFolders = null;
			mFullScanPending = false;
		}
	}

	public void cleanLibrary() {
		String[] projection = {MediaLibrary.AlbumColumns.ALBUM, MediaLibrary.SongColumns.TITLE, MediaLibrary.SongColumns.PATH};
		String source = MediaLibrary.TABLE_SONGS +
			" INNER JOIN " + MediaLibrary.TABLE_ALBUMS +
			" ON " + MediaLibrary.TABLE_SONGS + "." + MediaLibrary.SongColumns.ALBUM_ID +
			" = " + MediaLibrary.TABLE_ALBUMS + "." + MediaLibrary.AlbumColumns._ID;
		//String query = MediaLibrary.SongColumns.ALBUM_ID + "=?";
		//String[] queryArgs = new String[]{Long.toString(id)};

		Cursor cursor = MediaLibrary.queryLibrary(getContext(), source, projection, null, null, MediaLibrary.AlbumColumns.ALBUM);

		if (cursor != null) {

			int delCount = 0;

			while (cursor.moveToNext()) {
				String albumName = cursor.getString(0);
				String songName = cursor.getString(1);
				int rating = RatingDB.getSongRating(songName, albumName);
				if (rating == 1) {
					Log.v("Deleted", albumName + " - " + songName);
					File file = new File(cursor.getString(2));
					if (file.exists()) {
						if (file.delete()) {
							delCount++;
						}
					}
				}
			}

			Toast.makeText(getContext(), String.format(Locale.ROOT, "Deleted %d songs. It's recommended to use Start Scan now", delCount), Toast.LENGTH_LONG).show();

			cursor.close();
		}
	}

	public void refreshFavorites() {
		String[] projection = {MediaLibrary.AlbumColumns.ALBUM, MediaLibrary.SongColumns.TITLE, MediaLibrary.TABLE_SONGS + "." + MediaLibrary.SongColumns._ID};
		String source = MediaLibrary.TABLE_SONGS +
				" INNER JOIN " + MediaLibrary.TABLE_ALBUMS +
				" ON " + MediaLibrary.TABLE_SONGS + "." + MediaLibrary.SongColumns.ALBUM_ID +
				" = " + MediaLibrary.TABLE_ALBUMS + "." + MediaLibrary.AlbumColumns._ID;
		//String query = MediaLibrary.SongColumns.ALBUM_ID + "=?";
		//String[] queryArgs = new String[]{Long.toString(id)};

		Cursor cursor = MediaLibrary.queryLibrary(getContext(), source, projection, null, null, MediaLibrary.AlbumColumns.ALBUM);

		long bestOfTheBestId = Playlist.getBestOfTheBestId(getContext(), true);
		long favoritesId = Playlist.getFavoritesId(getContext(), true);
		long favoritesExId = Playlist.getFavoritesExId(getContext(), true);

		MediaLibrary.removePlaylist(getContext(), bestOfTheBestId);
		MediaLibrary.removePlaylist(getContext(), favoritesId);
		MediaLibrary.removePlaylist(getContext(), favoritesExId);

		bestOfTheBestId = Playlist.getBestOfTheBestId(getContext(), true);
		favoritesId = Playlist.getFavoritesId(getContext(), true);
		favoritesExId = Playlist.getFavoritesExId(getContext(), true);

		ArrayList<Long> array5 = new ArrayList<>();
		ArrayList<Long> array4 = new ArrayList<>();
		ArrayList<Long> array3 = new ArrayList<>();

		if (cursor != null) {

			while (cursor.moveToNext()) {
				String albumName = cursor.getString(0);
				String songName = cursor.getString(1);
				Long songID = cursor.getLong(2);
				int rating = RatingDB.getSongRating(songName, albumName);

				if (rating >= 5) { array5.add(songID); }
				if (rating >= 4) { array4.add(songID); }
				if (rating >= 3) { array3.add(songID); }
			}

			cursor.close();

			if (array5.size() > 0) {
				MediaLibrary.addToPlaylist(getContext(), bestOfTheBestId, array5);
			}

			if (array4.size() > 0) {
				MediaLibrary.addToPlaylist(getContext(), favoritesId, array4);
			}

			if (array3.size() > 0) {
				MediaLibrary.addToPlaylist(getContext(), favoritesExId, array3);
			}

			Toast.makeText(getContext(), "Finished refreshing favorites", Toast.LENGTH_LONG).show();
		}
	}

	public void cleanLibraryDialog() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						//Yes button clicked
						cleanLibrary();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMessage("Do you want to delete all songs rated 1 star?").setPositiveButton("Yes", dialogClickListener)
			.setNegativeButton("No", dialogClickListener).show();
	}

	public void refreshFavoritesDialog() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						//Yes button clicked
						refreshFavorites();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMessage("Do you want to refresh the favorite playlists?").setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.start_button:
				startButtonPressed(view);
				break;
			case R.id.cancel_button:
				cancelButtonPressed(view);
				break;
			case R.id.edit_button:
				editButtonPressed(view);
				break;
			case R.id.media_scan_group_albums:
			case R.id.media_scan_force_bastp:
				confirmUpdatePreferences((CheckBox)view);
				break;
			case R.id.clean_library:
				cleanLibraryDialog();
				break;
			case R.id.favorites_button:
				refreshFavoritesDialog();
				break;
		}
	}

	private static final int MENU_DUMP_DB = 1;
	private static final int MENU_FORCE_M3U_IMPORT = 2;
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.add(0, MENU_DUMP_DB, 30, R.string.dump_database);
		menu.add(0, MENU_FORCE_M3U_IMPORT, 30, R.string.force_m3u_import);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DUMP_DB:
			final Context context = getActivity();
			final String path = Environment.getExternalStorageDirectory().getPath() + "/dbdump-" + context.getPackageName() + ".sqlite";
			final String msg = getString(R.string.dump_database_result, path);

			MediaLibrary.createDebugDump(context, path);
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			break;
		case MENU_FORCE_M3U_IMPORT:
			// Sending an 'OUTDATED' event signals that our playlist information is wrong.
			// This should trigger a full re-import.
			MediaLibrary.notifyObserver(LibraryObserver.Type.PLAYLIST, LibraryObserver.Value.OUTDATED, false);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	/**
	 * Wrapper for updatePreferences() which warns the user about
	 * possible consequences.
	 *
	 * @param checkbox the checkbox which was changed
	 */
	private void confirmUpdatePreferences(final CheckBox checkbox) {
		if (mFullScanPending) {
			// User was already warned, so we can just dispatch this
			// without nagging again
			updatePreferences(checkbox);
			return;
		}

		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.media_scan_preferences_change_title)
			.setMessage(R.string.media_scan_preferences_change_message)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					mFullScanPending = true;
					confirmUpdatePreferences(checkbox);
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// restore old condition if use does not want to proceed
					checkbox.setChecked(!checkbox.isChecked());
				}
			})
			.show();
	}

	/**
	 * Initializes and updates the scanner preferences
	 *
	 * @param checkbox the item to update, may be null
	 * @return void but sets the checkboxes to their correct state
	 */
	private void updatePreferences(CheckBox checkbox) {
		MediaLibrary.Preferences prefs = MediaLibrary.getPreferences(getActivity());

		if (checkbox == mGroupAlbumsCheck)
			prefs.groupAlbumsByFolder = mGroupAlbumsCheck.isChecked();
		if (checkbox == mForceBastpCheck)
			prefs.forceBastp = mForceBastpCheck.isChecked();

		MediaLibrary.setPreferences(getActivity(), prefs);

		mGroupAlbumsCheck.setChecked(prefs.groupAlbumsByFolder);
		mForceBastpCheck.setChecked(prefs.forceBastp);
		mMediaDirectories.setText(getMediaFoldersDescription());
	}

	/**
	 * Returns a textual representation of the media folder state
	 *
	 * @return string describing our directory scan preferences
	 */
	private String getMediaFoldersDescription() {
		MediaLibrary.Preferences prefs = MediaLibrary.getPreferences(getActivity());
		String description = "";
		for (String path : prefs.mediaFolders) {
			description += "✔ " + path + "\n";
		}
		for (String path : prefs.blacklistedFolders) {
			description += "✘ " + path + "\n";
		}
		return description;
	}

	/**
	 * Updates the view of this fragment with current information
	 */
	private void updateProgress() {
		Context context = getActivity();
		MediaLibrary.ScanProgress progress = MediaLibrary.describeScanProgress(context);

		boolean idle = !progress.isRunning;
		mProgressText.setText(progress.lastFile);
		mProgressBar.setMax(progress.total);
		mProgressBar.setProgress(progress.seen);

		mStartButton.setEnabled(idle);
		mEditButton.setEnabled(idle);
		mDropDbCheck.setEnabled(idle);
		mFullScanCheck.setEnabled(idle);
		mForceBastpCheck.setEnabled(idle);
		mGroupAlbumsCheck.setEnabled(idle);

		mCancelButton.setVisibility(idle ? View.GONE : View.VISIBLE);
		mProgressText.setVisibility(idle ? View.GONE : View.VISIBLE);
		mProgressBar.setVisibility(idle ? View.GONE : View.VISIBLE);

		Integer songCount = MediaLibrary.getLibrarySize(context);
		mStatsTracks.setText(songCount.toString());

		float libraryPlaytime = calculateSongSum(context, MediaLibrary.SongColumns.DURATION) / 3600000F;
		mStatsLibraryPlaytime.setText(String.format("%.1f", libraryPlaytime));

		float listenPlaytime = calculateSongSum(context, MediaLibrary.SongColumns.PLAYCOUNT+"*"+MediaLibrary.SongColumns.DURATION) / 3600000F;
		mStatsListenPlaytime.setText(String.format("%.1f", listenPlaytime));
	}

	/**
	 * Queries the media library and calculates the sum of given column.
	 *
	 * @param context the context to use
	 * @param column the column to sum up
	 * @return the play time of the library in ms
	 */
	public long calculateSongSum(Context context, String column) {
		long duration = 0;
		Cursor cursor = MediaLibrary.queryLibrary(context, MediaLibrary.TABLE_SONGS, new String[]{"SUM("+column+")"}, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				duration = cursor.getLong(0);
			}
			cursor.close();
		}
		return duration;
	}

	/**
	 * Called when the user hits the start button
	 *
	 * @param view the view which was pressed
	 */
	public void startButtonPressed(View view) {
		MediaLibrary.startLibraryScan(getActivity(), mFullScanCheck.isChecked(), mDropDbCheck.isChecked());
		updateProgress();
	}

	/**
	 * Called when the user hits the cancel button
	 *
	 * @param view the view which was pressed
	 */
	public void cancelButtonPressed(View view) {
		MediaLibrary.abortLibraryScan(getActivity());
		updateProgress();
	}

	/**
	 * Called when the user hits the edit button
	 *
	 * @param view the view which was pressed
	 */
	private void editButtonPressed(final View view) {
		if (mFullScanPending) {
			// no need to nag if we are scanning anyway
			startMediaFoldersSelection();
			return;
		}

		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.media_scan_preferences_change_title)
			.setMessage(R.string.media_scan_preferences_change_message)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					startMediaFoldersSelection();
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {}
			})
			.show();
	}

	/**
	 * Launches the edit dialog
	 */
	private void startMediaFoldersSelection() {
			mIsEditingDirectories = true;
			startActivity(new Intent(getActivity(), MediaFoldersSelectionActivity.class));
	}

}
