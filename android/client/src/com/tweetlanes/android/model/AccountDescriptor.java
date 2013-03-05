/*
 * Copyright (C) 2013 Chris Lacy Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tweetlanes.android.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterConstant;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.model.TwitterList;
import org.tweetalib.android.model.TwitterLists;
import org.tweetalib.android.model.TwitterUser;

import android.graphics.Bitmap;

import com.tweetlanes.android.App;
import com.tweetlanes.android.Constant;
import com.tweetlanes.android.Constant.LaneType;
import com.tweetlanes.android.R;
import com.tweetlanes.android.URLFetch;
import com.tweetlanes.android.URLFetch.FetchBitmapCallback;

public class AccountDescriptor {

    static final String KEY_ID = "id";
    static final String KEY_SCREEN_NAME = "screenName";
    static final String KEY_OAUTH_TOKEN = "oAuthToken";
    static final String KEY_OAUTH_SECRET = "oAuthSecret";
    static final String KEY_INITIAL_LANE_INDEX = "lastLaneIndex";
    static final String KEY_LISTS = "lists";
    static final String KEY_LIST_ID = "id";
    static final String KEY_LIST_NAME = "name";
    static final String KEY_DISPLAYED_LANES = "displayedLanes";
    static final String KEY_SOCIAL_NET_TYPE = "socialNetType";

    /*
	 *
	 */
    public AccountDescriptor(TwitterUser user, String oAuthToken,
            String oAuthSecret, SocialNetConstant.Type oSocialNetType) {
        mId = user.getId();
        mScreenName = user.getScreenName();
        mOAuthToken = oAuthToken;
        mOAuthSecret = oAuthSecret;
        mInitialLaneIndex = null;
        mSocialNetType = oSocialNetType;
        initCommon(null);

    }

    /*
	 *
	 */
    public AccountDescriptor(String jsonAsString) {

        try {
            JSONObject object = new JSONObject(jsonAsString);
            mId = object.getLong(KEY_ID);
            mScreenName = object.getString(KEY_SCREEN_NAME);
            mOAuthToken = object.getString(KEY_OAUTH_TOKEN);
            if (object.has(KEY_OAUTH_SECRET)) {
                mOAuthSecret = object.getString(KEY_OAUTH_SECRET);
            }
            if (object.has(KEY_INITIAL_LANE_INDEX)) {
                mInitialLaneIndex = object.getInt(KEY_INITIAL_LANE_INDEX);
            } else {
                mInitialLaneIndex = null;
            }
            if (object.has(KEY_SOCIAL_NET_TYPE)) {
                mSocialNetType = SocialNetConstant.Type.valueOf((String) object
                        .get(KEY_SOCIAL_NET_TYPE));
            } else {
                mSocialNetType = SocialNetConstant.Type.Twitter;
            }
            if (object.has(KEY_LISTS)) {
                mLists = new ArrayList<List>();
                String listsAsString = object.getString(KEY_LISTS);
                JSONArray jsonArray = new JSONArray(listsAsString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    // JSONObject listObject = jsonArray.getJSONObject(i);
                    String listString = jsonArray.getString(i);
                    List list = new List(listString);
                    mLists.add(list);
                }

            }
            ArrayList<String> displayedLanes = new ArrayList<String>();
            if (object.has(KEY_DISPLAYED_LANES)) {
                String displayedLanedAsString = object
                        .getString(KEY_DISPLAYED_LANES);
                JSONArray jsonArray = new JSONArray(displayedLanedAsString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    String laneIdentifier = jsonArray.getString(i);
                    displayedLanes.add(laneIdentifier);
                }

            }
            initCommon(displayedLanes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
	 *
	 */
    private void initCommon(ArrayList<String> displayedLanes) {

        mShouldRefreshLists = true;

        mLaneDefinitions = new ArrayList<LaneDescriptor>();
        if (mLists == null) {
            mLists = new ArrayList<List>();
        }
        configureLaneDefinitions(displayedLanes);

        if (Constant.ENABLE_PROFILE_IMAGES) {

            FetchBitmapCallback callback = new FetchBitmapCallback() {

                @Override
                public void finished(boolean successful, Bitmap bitmap) {
                    if (successful == true && bitmap != null) {
                        mProfileImage = bitmap.copy(bitmap.getConfig(), false);
                    }
                }

            };
            URLFetch.fetchBitmap(
                    TwitterManager.get().getProfileImageUrl(mScreenName,
                            TwitterManager.ProfileImageSize.BIGGER), callback);
        }
    }

    /*
	 *
	 */
    private void configureLaneDefinitions(ArrayList<String> displayedLanes) {

        mLaneDefinitions.clear();

        mLaneDefinitions
                .add(new LaneDescriptor(Constant.LaneType.USER_PROFILE, App
                        .getContext().getString(R.string.lane_user_profile),
                        new TwitterContentHandleBase(
                                TwitterConstant.ContentType.USER)));
        mLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.USER_PROFILE_TIMELINE, App.getContext()
                        .getString(mSocialNetType == SocialNetConstant.Type.Twitter ? R.string.lane_user_tweets : R
                                .string.lane_user_tweets_adn),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.USER_TIMELINE)));

        mLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.RETWEETS_OF_ME, App.getContext()
                        .getString(mSocialNetType == SocialNetConstant.Type.Twitter ? R.string
                                .lane_user_retweets_of_me :  R.string.lane_user_retweets_of_me_adn),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.RETWEETS_OF_ME)));

        mLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.USER_HOME_TIMELINE, App.getContext()
                        .getString(R.string.lane_user_home),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.USER_HOME_TIMELINE)));

        mLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.USER_MENTIONS, App.getContext().getString(
                        R.string.lane_user_mentions),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.USER_MENTIONS)));

        if (mSocialNetType == SocialNetConstant.Type.Appdotnet) {
            mLaneDefinitions.add(new LaneDescriptor(
                    Constant.LaneType.GLOBAL_FEED, App.getContext().getString(
                            R.string.lane_user_global_feed),
                    new TwitterContentHandleBase(
                            TwitterConstant.ContentType.STATUSES,
                            TwitterConstant.StatusesType.GLOBAL_FEED)));
        }

        if (mSocialNetType == SocialNetConstant.Type.Twitter) {
            mLaneDefinitions.add(new LaneDescriptor(
                    Constant.LaneType.DIRECT_MESSAGES, App.getContext()
                            .getString(R.string.lane_direct_messages),
                    new TwitterContentHandleBase(
                            TwitterConstant.ContentType.DIRECT_MESSAGES,
                            TwitterConstant.DirectMessagesType.ALL_MESSAGES)));

            // Add lists
            for (List list : mLists) {
                if (list.mId != null) {
                    mLaneDefinitions
                            .add(new LaneDescriptor(
                                    Constant.LaneType.USER_LIST_TIMELINE,
                                    list.mName,
                                    String.valueOf(list.mId),
                                    new TwitterContentHandleBase(
                                            TwitterConstant.ContentType.STATUSES,
                                            TwitterConstant.StatusesType.USER_LIST_TIMELINE)));
                }
            }
        }

        // Add the final batch
        mLaneDefinitions.add(new LaneDescriptor(Constant.LaneType.FRIENDS, App
                .getContext().getString(R.string.lane_friends),
                new TwitterContentHandleBase(TwitterConstant.ContentType.USERS,
                        TwitterConstant.UsersType.FRIENDS)));
        mLaneDefinitions.add(new LaneDescriptor(Constant.LaneType.FOLLOWERS,
                App.getContext().getString(R.string.lane_followers),
                new TwitterContentHandleBase(TwitterConstant.ContentType.USERS,
                        TwitterConstant.UsersType.FOLLOWERS)));
        mLaneDefinitions.add(new LaneDescriptor(
                Constant.LaneType.USER_FAVORITES, App.getContext().getString(
                        R.string.lane_user_favorites),
                new TwitterContentHandleBase(
                        TwitterConstant.ContentType.STATUSES,
                        TwitterConstant.StatusesType.USER_FAVORITES)));

        if (displayedLanes != null && displayedLanes.size() > 0) {
            for (LaneDescriptor lane : mLaneDefinitions) {
                boolean display = false;
                for (String laneTitle : displayedLanes) {
                    if (lane.getLaneTitle().equals(laneTitle)) {
                        display = true;
                        break;
                    }
                }
                if (lane.getDisplay() != display) {
                    lane.setDisplay(display);
                    mLaneDefinitionsDirty = true;
                }
            }
        }
    }

    /*
	 *
	 */
    public boolean updateTwitterLists(TwitterLists twitterLists) {

        mShouldRefreshLists = false;
        if (mLists != null) {
            mLists.clear();
        } else {
            mLists = new ArrayList<List>();
        }
        boolean changed = false;
        if (twitterLists != null && twitterLists.getListCount() > 0) {
            for (int i = 0; i < twitterLists.getListCount(); i++) {
                TwitterList twitterList = twitterLists.getList(i);
                mLists.add(new List(twitterList));
                boolean exists = false;

                for (LaneDescriptor lane : mLaneDefinitions) {

                    if (lane.getLaneType() == LaneType.USER_LIST_TIMELINE) {
                        try {
                            String laneIdAsString = lane.getIdentifier();
                            Long id = Long.valueOf(laneIdAsString);
                            if (id == twitterList.getId()) {
                                exists = true;
                                if (lane.getLaneTitle().equals(
                                        twitterList.getName()) == false) {
                                    changed = true;
                                    lane.setLaneTitle(twitterList.getName());
                                }
                                break;
                            }

                        } catch (NumberFormatException e) {
                            changed = true;
                            break;
                        }
                    }
                }

                if (exists == false) {
                    changed = true;
                }
            }
        }

        if (changed == true) {
            configureLaneDefinitions(null);
        }

        return changed;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_ID, mId);
            object.put(KEY_SCREEN_NAME, mScreenName);
            object.put(KEY_OAUTH_TOKEN, mOAuthToken);
            object.put(KEY_OAUTH_SECRET, mOAuthSecret);
            object.put(KEY_INITIAL_LANE_INDEX, mInitialLaneIndex);
            object.put(KEY_SOCIAL_NET_TYPE, mSocialNetType);

            if (mLists.size() > 0) {
                JSONArray listArray = new JSONArray();
                for (List list : mLists) {
                    listArray.put(list.toString());
                }
                object.put(KEY_LISTS, listArray);
            }

            if (mLaneDefinitions != null && mLaneDefinitions.size() > 0) {
                JSONArray laneDisplayArray = new JSONArray();
                for (LaneDescriptor lane : mLaneDefinitions) {
                    if (lane.getDisplay()) {
                        laneDisplayArray.put(lane.getLaneTitle());
                    }
                }
                object.put(KEY_DISPLAYED_LANES, laneDisplayArray);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    /*
	 *
	 */
    public long getId() {
        return mId;
    }

    public String getScreenName() {
        return mScreenName;
    }

    public String getOAuthToken() {
        return mOAuthToken;
    }

    public String getOAuthSecret() {
        return mOAuthSecret;
    }

    public Bitmap getProfileImage() {
        return Constant.ENABLE_PROFILE_IMAGES ? mProfileImage : null;
    }

    /*
	 *
	 */
    public int getInitialLaneIndex() {
        if (mInitialLaneIndex != null
                && mInitialLaneIndex.intValue() < getDisplayedLaneDefinitionsSize()) {
            return mInitialLaneIndex;
        }

        int displayIndex = 0;
        for (LaneDescriptor lane : mLaneDefinitions) {
            if (lane.getLaneType() == LaneType.USER_HOME_TIMELINE) {
                return displayIndex;
            }
            displayIndex += 1;
        }

        return 0;
    }

    /*
	 *
	 */
    public void setCurrentLaneIndex(int index) {
        mInitialLaneIndex = index;
    }

    /*
	 *
	 */
    public ArrayList<LaneDescriptor> getAllLaneDefinitions() {
        return mLaneDefinitions;
    }

    /*
	 *
	 */
    public LaneDescriptor getDisplayedLaneDefinition(int index) {

        int displayedSize = 0;
        for (LaneDescriptor lane : mLaneDefinitions) {
            if (lane.getDisplay()) {
                if (displayedSize == index) {
                    return lane;
                }
                displayedSize += 1;
            }
        }
        return null;
    }

    /*
	 *
	 */
    public int getDisplayedLaneDefinitionsSize() {
        int displayedSize = 0;
        for (LaneDescriptor lane : mLaneDefinitions) {
            if (lane.getDisplay()) {
                displayedSize += 1;
            }
        }
        return displayedSize;
    }

    /*
	 *
	 */
    public boolean getDisplayedLaneDefinitionsDirty() {
        return mLaneDefinitionsDirty;
    }

    /*
	 *
	 */
    public void setDisplayedLaneDefinitionsDirty(boolean value) {
        mLaneDefinitionsDirty = value;
    }

    public SocialNetConstant.Type getSocialNetType() {
        return mSocialNetType;
    }

    public void setSocialNetType(SocialNetConstant.Type mSocialNetType) {
        this.mSocialNetType = mSocialNetType;
    }

    /*
	 *
	 */
    public boolean shouldRefreshLists() {
        return mShouldRefreshLists;
    }

    /*
	 *
	 */
    private long mId;
    private String mScreenName;
    private String mOAuthToken;
    private String mOAuthSecret;
    private Bitmap mProfileImage; // Of size
    // TwitterManager.ProfileImageSize.BIGGER
    private ArrayList<LaneDescriptor> mLaneDefinitions;
    private boolean mLaneDefinitionsDirty;
    private Integer mInitialLaneIndex;
    private ArrayList<List> mLists;
    private boolean mShouldRefreshLists;
    private SocialNetConstant.Type mSocialNetType;

    /*
     * Stripped version of the List class. Possibly should use TwitterList, but
     * I thought I thought it best to save the string space of that much larger
     * structure
     */
    private class List {

        List(TwitterList twitterList) {
            mId = twitterList.getId();
            mName = twitterList.getName();
        }

        List(String jsonAsString) {
            try {
                JSONObject object = new JSONObject(jsonAsString);
                mId = object.getInt(KEY_LIST_ID);
                mName = object.getString(KEY_LIST_NAME);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public String toString() {
            JSONObject object = new JSONObject();
            try {
                object.put(KEY_LIST_NAME, mName);
                object.put(KEY_LIST_ID, mId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object.toString();
        }

        Integer mId;
        String mName;
    }
}
