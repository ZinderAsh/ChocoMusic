package ch.blinkenlights.android.chocolate;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RatingDB {

	private static final File json_file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "chocolate.json");

	private static JSONObject json_obj = null;

	public static void unload() {
		json_obj = null;
	}

	public static boolean createJSONFile() throws IOException, JSONException {
		json_file.getParentFile().mkdirs();
		if (!json_file.createNewFile()) {
			return false;
		}
		JSONObject jsonObject = new JSONObject();
		JSONArray array = new JSONArray();
		jsonObject.put("album", array);
		writeJSONObject(jsonObject);
		return true;
	}

	public static JSONObject getJSONObject() throws IOException, JSONException {
		if (json_obj != null) {
			return json_obj;
		}
		if (!json_file.exists()) {
			if (!createJSONFile()) {
				return null;
			}
		}
		Scanner scanner = new Scanner(json_file);
		String content = "";
		while (scanner.hasNextLine()) {
			content += scanner.nextLine();
		}
		scanner.close();

		JSONObject json;
		json = new JSONObject(content);
		if (!json.has("album")) {
			JSONArray array = new JSONArray();
			json.put("album", array);
			writeJSONObject(json);
		}

		json_obj = json;

		return json_obj;
	}

	public static boolean writeJSONObject(JSONObject json) {
		String string = json.toString();
		try {
			FileOutputStream fOut = new FileOutputStream(json_file);
			OutputStreamWriter writer = new OutputStreamWriter(fOut);
			writer.append(string);
			writer.close();
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}


	public static int[] getSongRatings(List<Song> songList) {
		int[] ratings = new int[songList.size()];
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		if (json == null) {
			return null;
		}
		try {
			int found = 0;
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				JSONArray songs = albumObj.getJSONArray("song");
				for (int j = 0; j < songs.length(); j++) {
					JSONObject songObj = songs.getJSONObject(j);
					for (int k = 0; k < songList.size(); k++) {
						Song s = songList.get(k);
						if (songObj.getString("title").equals(s.title) &&
							albumObj.getString("name").equals(s.album)) {
							ratings[k] = songObj.has("score") ? songObj.getInt("score") : 0;
							found++;
							if (found >= songList.size()) {
								return ratings;
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return ratings;
	}


	public static int[] getSongTypes(List<Song> songList) {
		int[] types = new int[songList.size()];
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		if (json == null) {
			return null;
		}
		try {
			int found = 0;
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				JSONArray songs = albumObj.getJSONArray("song");
				for (int j = 0; j < songs.length(); j++) {
					JSONObject songObj = songs.getJSONObject(j);
					for (int k = 0; k < songList.size(); k++) {
						Song s = songList.get(k);
						if (songObj.getString("title").equals(s.title) &&
							albumObj.getString("name").equals(s.album)) {
							types[k] = songObj.has("vocal") ? songObj.getInt("vocal") : 0;
							found++;
							if (found >= songList.size()) {
								return types;
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return types;
	}


	public static double getAlbumRating(String album) {
		// Weighs ratings towards higher ratings the higher this number is
		final double power = 6.0;
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				return 0;
			}
			return 0;
		} catch (JSONException e) {
			return 0;
		}
		if (json == null) {
			return 0;
		}
		try {
			boolean found_one = false;
			int count = 0;
			double total = 0;
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.has("score")) {
							int score = songObj.getInt("score");
							if (score == 1) {
								found_one = true;
							} else if (score > 1) {
								count++;
								total += Math.pow(score, power);
							}
						}
					}
					if (count <= 0) {
						if (found_one) return 1;
						return 0;
					} else {
						return Math.pow(total / count, 1.0 / power);
					}
				}
			}
		} catch (JSONException e) {
			return 0;
		}
		return 0;
	}

	public static int getSongRating(String title, String album) {
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				return -5;
			}
			return -3;
		} catch (JSONException e) {
			return -4;
		}
		if (json == null) {
			return -1;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.getString("title").equals(title)) {
							if (songObj.has("score")) {
								return songObj.getInt("score");
							} else {
								return 0;
							}
						}
					}
					return 0;
				}
			}
		} catch (JSONException e) {
			return -2;
		}
		return 0;
	}

	public static int getSongRating(Song song) {
		if (song == null) {
			return 0;
		}
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				return -5;
			}
			return -3;
		} catch (JSONException e) {
			return -4;
		}
		if (json == null) {
			return -1;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(song.album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.getString("title").equals(song.title)) {
							if (songObj.has("score")) {
								return songObj.getInt("score");
							} else {
								return 0;
							}
						}
					}
					return 0;
				}
			}
		} catch (JSONException e) {
			return -2;
		}
		return 0;
	}

	public static int setSongRating(Song song, int rating) {
		if (song == null) {
			return 0;
		}
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			return -3;
		} catch (JSONException e) {
			return -4;
		}
		if (json == null) {
			return -1;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(song.album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.getString("title").equals(song.title)) {
							// Song already exists in metadata
							songObj.put("score", rating);
							writeJSONObject(json);
							return 0;
						}
					}
					// Album exists, but not song
					JSONObject songObj = new JSONObject();
					songObj.put("title", song.title);
					songObj.put("score", rating);
					songs.put(songObj);
					writeJSONObject(json);
					return 0;
				}
			}
			// Neither album nor song exist in metadata
			JSONObject albumObj = new JSONObject();
			albumObj.put("name", song.album);
			JSONArray songs = new JSONArray();
			JSONObject songObj = new JSONObject();
			songObj.put("title", song.title);
			songObj.put("score", rating);
			songs.put(songObj);
			albumObj.put("song", songs);
			albums.put(albumObj);
			writeJSONObject(json);
			return 0;
		} catch (JSONException e) {
			return -2;
		}
	}

	public static int getSongType(Song song) {
		if (song == null) {
			return 0;
		}
		JSONObject json = null;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				return -5;
			}
			return -3;
		} catch (JSONException e) {
			return -4;
		}
		if (json == null) {
			return -1;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(song.album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.getString("title").equals(song.title)) {
							if (songObj.has("vocal")) {
								return songObj.getInt("vocal");
							} else {
								return 0;
							}
						}
					}
					return 0;
				}
			}
		} catch (JSONException e) {
			return -2;
		}
		return 0;
	}


	public static int setSongType(Song song, int type) {
		if (song == null) {
			return 0;
		}
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			return -3;
		} catch (JSONException e) {
			return -4;
		}
		if (json == null) {
			return -1;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(song.album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.getString("title").equals(song.title)) {
							// Song already exists in metadata
							songObj.put("vocal", type);
							writeJSONObject(json);
							return 0;
						}
					}
					// Album exists, but not song
					JSONObject songObj = new JSONObject();
					songObj.put("title", song.title);
					songObj.put("vocal", type);
					songs.put(songObj);
					writeJSONObject(json);
					return 0;
				}
			}
			// Neither album nor song exist in metadata
			JSONObject albumObj = new JSONObject();
			albumObj.put("name", song.album);
			JSONArray songs = new JSONArray();
			JSONObject songObj = new JSONObject();
			songObj.put("title", song.title);
			songObj.put("vocal", type);
			songs.put(songObj);
			albumObj.put("song", songs);
			albums.put(albumObj);
			writeJSONObject(json);
			return 0;
		} catch (JSONException e) {
			return -2;
		}
	}


	public static int setAlbumSongType(String album, ArrayList<Song> songList, int type) {
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			return -3;
		} catch (JSONException e) {
			return -4;
		}
		if (json == null) {
			return -1;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (Song s : songList) {
						boolean found = false;
						for (int j = 0; j < songs.length(); j++) {
							JSONObject songObj = songs.getJSONObject(j);
							if (songObj.getString("title").equals(s.title)) {
								// Song already exists in metadata
								songObj.put("vocal", type);
								writeJSONObject(json);
								found = true;
							}
						}
						if (!found) {
							// Album exists, but not song
							JSONObject songObj = new JSONObject();
							songObj.put("title", s.title);
							songObj.put("vocal", type);
							songs.put(songObj);
							writeJSONObject(json);
						}
					}
					return 0;
				}
			}
			// Neither album nor song exist in metadata
			JSONObject albumObj = new JSONObject();
			albumObj.put("name", album);
			JSONArray songs = new JSONArray();
			for (Song s : songList) {
				JSONObject songObj = new JSONObject();
				songObj.put("title", s.title);
				songObj.put("vocal", type);
				songs.put(songObj);
			}
			albumObj.put("song", songs);
			albums.put(albumObj);
			writeJSONObject(json);
			return 0;
		} catch (JSONException e) {
			return -2;
		}
	}

	public static MetaBundle getSongMetaBundle(Song song) {
		if (song == null) {
			return new MetaBundle(0, 0);
		}
		JSONObject json;
		try {
			json = getJSONObject();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		if (json == null) {
			return null;
		}
		try {
			JSONArray albums = json.getJSONArray("album");
			for (int i = 0; i < albums.length(); i++) {
				JSONObject albumObj = albums.getJSONObject(i);
				if (albumObj.getString("name").equals(song.album)) {
					JSONArray songs = albumObj.getJSONArray("song");
					for (int j = 0; j < songs.length(); j++) {
						JSONObject songObj = songs.getJSONObject(j);
						if (songObj.getString("title").equals(song.title)) {
							int rating = 0;
							int type = 0;
							if (songObj.has("score")) {
								rating = songObj.getInt("score");
							}
							if (songObj.has("vocal")) {
								type = songObj.getInt("vocal");
							}
							return new MetaBundle(rating, type);
						}
					}
					return new MetaBundle(0, 0);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return new MetaBundle(0, 0);
	}

	public static class MetaBundle {

		public final int rating;
		public final int type;

		public MetaBundle(int rating, int type) {
			this.rating = rating;
			this.type = type;
		}

	}

}
