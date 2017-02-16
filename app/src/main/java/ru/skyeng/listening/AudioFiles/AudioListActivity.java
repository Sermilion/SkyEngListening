package ru.skyeng.listening.AudioFiles;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.skyeng.listening.AudioFiles.model.AudioFile;
import ru.skyeng.listening.AudioFiles.model.AudioFilesRequestParams;
import ru.skyeng.listening.AudioFiles.player.PlayerService;
import ru.skyeng.listening.Categories.CategoriesActivity;
import ru.skyeng.listening.CommonComponents.BaseActivity;
import ru.skyeng.listening.R;

import static ru.skyeng.listening.AudioFiles.player.PlayerService.ACTION_AUDIO_STATE;
import static ru.skyeng.listening.AudioFiles.player.PlayerService.ACTION_PAUSE;
import static ru.skyeng.listening.AudioFiles.player.PlayerService.EXTRA_AUDIO_URL;
import static ru.skyeng.listening.AudioFiles.player.PlayerService.KEY_PLAYER_STATE;
import static ru.skyeng.listening.AudioFiles.player.PlayerService.PROGRESS_BAR_MAX;

public class AudioListActivity extends BaseActivity {

    private static final String TAG_AUDIO_FILES_FRAGMENT = AudioListFragment.class.getName();
    private static final String KEY_AUDIO_FILE = "mAudioFile";
    private static final String KEY_PROGRESS_VISIBILITY = "progressVisibility";
    public static final int TAG_REQUEST_CODE = 0;
    public static final String TAG_REQUEST_DATA = "tagExtra";

    public static boolean categoriesSelected = false;

    private AudioListFragment mFragment;
    private BottomSheetBehavior mBottomSheetBehavior;
    private AudioFile mAudioFile;
    private List<Integer> mSelectedTags;

    private boolean broadcastUpdateFinished;
    private AudioReceiver mPlayerBroadcast;
    boolean mBound = false;
    Messenger msgService;

    @BindView(R.id.appBarLayout)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.player_dialog)
    RelativeLayout mLayoutBottomSheet;
    @BindView(R.id.audio_cover)
    ImageView audioCoverImage;
    @BindView(R.id.audio_title)
    TextView audioTitle;
    @BindView(R.id.audio_left)
    TextView audioLeft;
    @BindView(R.id.audio_seek)
    SeekBar audioSeek;
    @BindView(R.id.audio_played)
    TextView audioPlayed;
    @BindView(R.id.audio_subtitles)
    TextView audioSubtitles;
    @BindView(R.id.audio_play_pause)
    ImageView audioPlayPause;
    @BindView(R.id.cover_dark_layer)
    View mDarkLayer;
    @BindView(R.id.button_length)
    Button mLengthButton;
    @BindView(R.id.button_category)
    Button mCategoryButton;
    @BindView(R.id.progressBar)
    ProgressBar mAudioProgressBar;
    @BindView(R.id.no_content_found)
    RelativeLayout mNoContentFoundLayout;
    @BindView(R.id.text_try)
    TextView mResetCategories;

    public RelativeLayout getNoContentFoundLayout() {
        return mNoContentFoundLayout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setupToolbar(getString(R.string.Listening), false);
        mFragment = (AudioListFragment) setupRecyclerFragment(
                savedInstanceState,
                AudioListFragment.class,
                R.id.fragment_container
        );
        mBottomSheetBehavior = BottomSheetBehavior.from(mLayoutBottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        setupPlayPlayer();
        restoreSavedInstanceState(savedInstanceState);
        mProgress = (ProgressBar) findViewById(R.id.loadingView);
        mAudioProgressBar.setIndeterminate(true);
        mAudioProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.colorWhite), android.graphics.PorterDuff.Mode.MULTIPLY);

//        audioSeek.setOnSeekBarChangeListener(mComponentListener);
        audioSeek.setMax(PROGRESS_BAR_MAX);
        mCategoryButton.setOnClickListener(
                v -> {
                    Intent intent = new Intent(AudioListActivity.this, CategoriesActivity.class);
                    Gson gson = new Gson();
                    String jsonTags = gson.toJson(mSelectedTags);
                    intent.putExtra(TAG_REQUEST_DATA, jsonTags);
                    startActivityForResult(intent, TAG_REQUEST_CODE);
                });
        mResetCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragment.setRequestParams(new AudioFilesRequestParams());
                mNoContentFoundLayout.setVisibility(View.GONE);
                mFragment.loadData(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAG_REQUEST_CODE && resultCode == RESULT_OK) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Integer>>() {
            }.getType();
            mSelectedTags = gson.fromJson(data.getStringExtra(TAG_REQUEST_DATA), type);
            AudioFilesRequestParams params = new AudioFilesRequestParams();
            params.setTagIds(mSelectedTags);
            mFragment.setRequestParams(params);
            mFragment.loadData(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mFragment != null) {
            getSupportFragmentManager().putFragment(outState, TAG_AUDIO_FILES_FRAGMENT, mFragment);
        }
        outState.putParcelable(KEY_AUDIO_FILE, mAudioFile);
        outState.putInt(KEY_PROGRESS_VISIBILITY, mAudioProgressBar.getVisibility());
        super.onSaveInstanceState(outState);
    }

    private void restoreSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mAudioFile = savedInstanceState.getParcelable(KEY_AUDIO_FILE);
            if (mAudioFile != null) {
                startPlaying(mAudioFile, false);
            }
            int visibility = savedInstanceState.getInt(KEY_PROGRESS_VISIBILITY, -1);
            if (!broadcastUpdateFinished) {
                if (visibility == View.VISIBLE) {
                    mAudioProgressBar.setVisibility(View.VISIBLE);
                    audioPlayPause.setVisibility(View.GONE);
                } else if (visibility == View.GONE) {
                    mAudioProgressBar.setVisibility(View.GONE);
                    audioPlayPause.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    private void setupPlayPlayer() {
        audioCoverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFragment.mAdapter.getPlayingPosition() != -1) {
                    int audioState = mFragment.getPresenter().getData().get(mFragment.mAdapter.getPlayingPosition()).getState();
                    int buttonIcon = R.drawable.ic_pause_white;
                    if (audioState == 1) {
                        buttonIcon = R.drawable.ic_play_white;
                        pausePlayerIntent(buttonIcon);
                        mFragment.mAdapter.setPlayerState(2);
                    } else {
                        startPlayerService(mAudioFile.getAudioFileUrl());
                        mFragment.mAdapter.setPlayerState(1);
                    }
                    if (audioState == 0) {
                        mDarkLayer.setVisibility(View.GONE);
                        audioPlayPause.setVisibility(View.VISIBLE);
                    } else {
                        mDarkLayer.setVisibility(View.VISIBLE);
                    }
                    audioPlayPause.setImageDrawable(ContextCompat.getDrawable(AudioListActivity.this, buttonIcon));
                }
            }
        });
    }

    public void startPlaying(AudioFile item, boolean isNew) {
        try {
            if (isNew) {
                startPlayerService(item.getAudioFileUrl());
            }
            mDarkLayer.setVisibility(View.VISIBLE);
            mAudioFile = item;
            audioTitle.setText(item.getTitle());
            audioPlayed.setText(getString(R.string.audio_default_time));
            audioLeft.setText("-" + item.getDurationInMinutes());
            if (item.getImageBitmap() != null) {
                audioCoverImage.setImageBitmap(item.getImageBitmap());
            }
            audioPlayPause.setVisibility(View.VISIBLE);
            int playPauseIcon = R.drawable.ic_play_white;
            if (item.getState() == 1) {
                playPauseIcon = R.drawable.ic_pause_white;
            }
            audioPlayPause.setImageDrawable(ContextCompat.getDrawable(this, playPauseIcon));
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPlayerService(String audioUrl) {
        bindPlayerService();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_AUDIO_URL, audioUrl);
        sendMessage(bundle);
    }

    public void pausePlayerIntent(int icon) {
        Intent intent = new Intent(ACTION_PAUSE);
        sendBroadcast(intent);
        audioPlayPause.setImageDrawable(ContextCompat.getDrawable(this, icon));
    }


    public void updateButtonsVisibility() {
        mLengthButton.setText(getString(R.string.length));
        mCategoryButton.setText(getString(R.string.categories));
    }

    private class AudioReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(KEY_PLAYER_STATE, false)) {
                mAudioProgressBar.setVisibility(View.VISIBLE);
                audioPlayPause.setVisibility(View.GONE);
            } else {
                mAudioProgressBar.setVisibility(View.GONE);
                audioPlayPause.setVisibility(View.VISIBLE);
            }
            broadcastUpdateFinished = true;
        }
    }

    @Override
    public void onResume() {
        if (!categoriesSelected) {
            if (mSelectedTags != null)
                mSelectedTags.clear();
            mFragment.loadData(false);
        }
        mPlayerBroadcast = new AudioReceiver();
        registerReceiver(mPlayerBroadcast, new IntentFilter(ACTION_AUDIO_STATE));
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mPlayerBroadcast != null) {
            unregisterReceiver(mPlayerBroadcast);
        }
        if (mBound) {
            unbindService(playerConnection);
            mBound = false;
        }
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindPlayerService();
    }

    public void sendMessage(Bundle bundle) {
        if (mBound) {
            try {
                Message message = Message.obtain(null, PlayerService.MESSAGE, 1, 1);
                message.replyTo = replyMessenger;
                message.setData(bundle);
                msgService.send(message); //sending message to service
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public ServiceConnection playerConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            mBound = true;
            msgService = new Messenger(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };

    Messenger replyMessenger = new Messenger(new HandlerReplyMsg());

    static class HandlerReplyMsg extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recdMessage = msg.obj.toString();
        }
    }

    private void showServiceData() {
        //call method od service
    }

//    public static Handler myHandler = new Handler() {
//        public void handleMessage(Message message) {
//            Bundle data = message.getData();
//        }
//    };

    public void bindPlayerService() {
        Intent intent = new Intent(this, PlayerService.class);
//        Messenger messenger = new Messenger(myHandler);
//        intent.putExtra("MESSENGER", messenger);
        bindService(intent, playerConnection, Context.BIND_AUTO_CREATE);
    }

}
