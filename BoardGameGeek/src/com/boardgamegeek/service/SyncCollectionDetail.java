package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionDetail extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetail.class);

	private static final int GAMES_PER_FETCH = 25;
	// TODO Perhaps move these constants into preferences
	private static final int SYNC_GAME_AGE_IN_DAYS = 30;
	private static final int SYNC_GAME_LIMIT = 25;

	private RemoteExecutor mRemoteExecutor;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing collection detail...");

		Cursor cursor = null;
		mRemoteExecutor = executor;
		ContentResolver resolver = executor.getContext().getContentResolver();

		try {
			long days = System.currentTimeMillis() - (SYNC_GAME_AGE_IN_DAYS * DateUtils.DAY_IN_MILLIS);
			cursor = resolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID }, SyncColumns.UPDATED + "<? OR "
				+ SyncColumns.UPDATED + " IS NULL", new String[] { String.valueOf(days) }, null);
			if (cursor.getCount() > 0) {
				LOGI(TAG, "Updating games older than " + SYNC_GAME_AGE_IN_DAYS + " days old");
				fetchGames(cursor, syncResult);
			} else {
				LOGI(TAG, "Updating " + SYNC_GAME_LIMIT + " oldest games");
				cursor.close();
				cursor = resolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID }, null, null, Games.UPDATED
					+ " LIMIT " + SYNC_GAME_LIMIT);
				fetchGames(cursor, syncResult);
			}
		} finally {
			LOGI(TAG, "Syncing collection detail complete.");
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void fetchGames(Cursor cursor, SyncResult syncResult) throws IOException, XmlPullParserException {
		List<String> ids = new ArrayList<String>();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			ids.add(cursor.getString(0));
			if (ids.size() >= GAMES_PER_FETCH) {
				fetchGames(ids, syncResult);
				ids.clear();
			}
		}
		fetchGames(ids, syncResult);
	}

	private void fetchGames(List<String> ids, SyncResult syncResult) throws IOException, XmlPullParserException {
		if (ids != null && ids.size() > 0) {
			RemoteBggHandler handler = new RemoteGameHandler();
			mRemoteExecutor.executeGet(HttpUtils.constructGameUrl(ids), handler);
			syncResult.stats.numUpdates += handler.getCount();
		}
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_detail;
	}
}
