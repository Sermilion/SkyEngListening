package ru.skyeng.listening.Modules.AudioFiles;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.makeramen.roundedimageview.RoundedImageView;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.skyeng.listening.CommonComponents.BaseActivity;
import ru.skyeng.listening.CommonComponents.EndlessRecyclerViewScrollListener;
import ru.skyeng.listening.CommonComponents.FacadeCommon;
import ru.skyeng.listening.CommonComponents.SEApplication;
import ru.skyeng.listening.MVPBase.MVPView;
import ru.skyeng.listening.Modules.AudioFiles.model.AudioData;
import ru.skyeng.listening.Modules.AudioFiles.model.AudioFile;
import ru.skyeng.listening.Modules.AudioFiles.model.SubtitlesRequestParams;
import ru.skyeng.listening.Modules.AudioFiles.network.AudioFilesService;
import ru.skyeng.listening.Modules.AudioFiles.network.SubtitlesService;
import ru.skyeng.listening.Modules.AudioFiles.player.PlayerService;
import ru.skyeng.listening.Modules.AudioFiles.player.PlayerState;
import ru.skyeng.listening.Modules.Categories.CategoriesActivity;
import ru.skyeng.listening.Modules.Settings.SettingsActivity;
import ru.skyeng.listening.Modules.Settings.model.SettingsObject;
import ru.skyeng.listening.R;
import ru.skyeng.listening.Utility.FacadePreferences;
import ru.skyeng.listening.Utility.asynctask.CommonAsyncTask;

import static ru.skyeng.listening.Modules.AudioFiles.player.PlayerService.ACTION_AUDIO_STATE;
import static ru.skyeng.listening.Modules.AudioFiles.player.PlayerService.ACTION_DID_NOT_STAR;
import static ru.skyeng.listening.Modules.AudioFiles.player.PlayerService.BINDER_MESSENGER;
import static ru.skyeng.listening.Modules.AudioFiles.player.PlayerService.EXTRA_AUDIO_URL;
import static ru.skyeng.listening.Modules.AudioFiles.player.PlayerService.KEY_PLAYER_STATE;

public class AudioListActivity extends BaseActivity<MVPView, AudioListPresenter> implements SwipeRefreshLayout.OnRefreshListener, MVPView {

    private static final String KEY_AUDIO_FILE = "mAudioFile";
    private static final String KEY_PROGRESS_VISIBILITY = "progressVisibility";
    public static final int TAG_REQUEST_CODE = 0;
    public static final String TAG_REQUEST_DATA = "tagExtra";
    private static final String KEY_SERVICE_BOUND = "serviceBound";

    protected Toolbar mToolbar;

    @Override
    @Inject
    public void setPresenter(@NonNull AudioListPresenter presenter) {
        this.presenter = presenter;
        super.setPresenter(presenter);
    }

    @Inject
    void setModel(AudioListModel model) {
        presenter.setModel(model);
    }

    @Inject
    void setRetrofitService(AudioFilesService audioFilesService) {
        ((AudioListModel) presenter.getModel()).setRetrofitService(audioFilesService);
    }

    @Inject
    void setSubtitleModel(SubtitlesModel model) {
        presenter.setSubtitlesModel(model);
    }

    @Inject
    void setSubtitlesService(SubtitlesService service) {
        ((SubtitlesModel) presenter.getSubtitlesModel()).setRetrofitService(service);
    }

    public static boolean categoriesSelected = false;
    private BottomSheetBehavior mBottomSheetBehavior;
    private AudioFile mAudioFile;
    private List<Integer> mSelectedTags;

    private boolean broadcastUpdateFinished;
    private AudioReceiver mPlayerBroadcast;
    boolean mBound = false;
    private Messenger msgService;
    private EndlessScrollListener mScrollListener;
    private AudioListAdapter mAdapter;

    @BindView(R.id.appBarLayout)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.player_dialog)
    RelativeLayout mLayoutBottomSheet;
    @BindView(R.id.audio_cover)
    RoundedImageView audioCoverImage;
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
    @BindView(R.id.action_settings)
    ImageButton mSettingsButton;
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout swipeContainer;

    public RelativeLayout getNoContentFoundLayout() {
        return mNoContentFoundLayout;
    }

    public void hideNoContentView(){
        mNoContentFoundLayout.setVisibility(View.GONE);
    }

    public void showNoContentView(){
        mNoContentFoundLayout.setVisibility(View.VISIBLE);
    }

    public boolean modelHasData() {
        if (presenter != null && presenter.getData() != null) {
            return presenter.getData().size() > 0;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((SEApplication) getApplicationContext()).getAudioListDiComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar(getString(R.string.Listening));
        mProgress = (ProgressBar) findViewById(R.id.loadingView);

        ButterKnife.bind(this);
        mAdapter = new AudioListAdapter(presenter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mScrollListener = new EndlessScrollListener(mLayoutManager);
        mRecyclerView.addOnScrollListener(mScrollListener);
        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(this.mRecyclerView.getContext(), mLayoutManager.getOrientation());
        this.mRecyclerView.addItemDecoration(mDividerItemDecoration);
        if ((presenter.getModel()).getItems() == null) {
            loadData(false);
        }

        swipeContainer.setColorSchemeResources(R.color.colorAccent);
        swipeContainer.setOnRefreshListener(this);

        startService(new Intent(this, PlayerService.class));

        mBottomSheetBehavior = BottomSheetBehavior.from(mLayoutBottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        restoreSavedInstanceState(savedInstanceState);
        setupPlayerCoverListener();
        mProgress = (ProgressBar) findViewById(R.id.loadingView);
        mAudioProgressBar.setIndeterminate(true);
        mAudioProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.colorWhite), android.graphics.PorterDuff.Mode.MULTIPLY);
        setupListeners();
    }

    private void setupListeners() {
        mCategoryButton.setOnClickListener(
                v -> {
                    Intent intent = new Intent(AudioListActivity.this, CategoriesActivity.class);
                    Gson gson = new Gson();
                    String jsonTags = gson.toJson(mSelectedTags);
                    intent.putExtra(TAG_REQUEST_DATA, jsonTags);
                    startActivityForResult(intent, TAG_REQUEST_CODE);
                });

        mLengthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsObject settings = FacadePreferences.getSettingsFromPref(AudioListActivity.this);
                showDurationPicker(settings);
            }
        });

        mResetCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNoContentFoundLayout.setVisibility(View.GONE);
                loadData(false);
            }
        });

        audioSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int currentProgress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentProgress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.sendMessage(null, PlayerService.MESSAGE_PLAYBACK_SEARCH, (long) currentProgress * 1000);
            }
        });
        mSettingsButton.setOnClickListener(v -> startActivity(new Intent(AudioListActivity.this, SettingsActivity.class)));
    }

    private void showDurationPicker(SettingsObject settings) {
        if (settings == null) {
            settings = new SettingsObject();
        }
        CharSequence durations[] = new CharSequence[]{
                getString(R.string.from0to5),
                getString(R.string.from5to10),
                getString(R.string.from10to20),
                getString(R.string.from20andMore)};

        AlertDialog.Builder builder = new AlertDialog.Builder(AudioListActivity.this);
        SettingsObject finalSettings = settings;
        boolean[] selected = settings.getDurationsBooleanArray();
        builder.setTitle(R.string.select_duration)
                .setMultiChoiceItems(durations, selected, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        selected[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.select, (dialog, id) -> {
                            finalSettings.setDuration(selected);
                            CommonAsyncTask<Void, Void, Void> saveSettingsTask = new CommonAsyncTask<>();
                            saveSettingsTask.setDoInBackground(param -> {
                                FacadePreferences.setSettingsToPref(AudioListActivity.this, finalSettings);
                                return null;
                            });
                            saveSettingsTask.setConsumer(param -> {
                                showToast(R.string.settings_saved);
                                loadData(false);
                            });
                            saveSettingsTask.execute();
                        }
                ).setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAG_REQUEST_CODE && resultCode == RESULT_OK) {
            mNoContentFoundLayout.setVisibility(View.GONE);
            Gson gson = new Gson();
            Type type = new TypeToken<List<Integer>>() {
            }.getType();
            mSelectedTags = gson.fromJson(data.getStringExtra(TAG_REQUEST_DATA), type);
            presenter.getModel().getRequestParams().setTagIds(mSelectedTags);
            getPresenter().clear();
            presenter.getModel().getRequestParams().setPage(1);
            loadData(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SERVICE_BOUND, mBound);
        outState.putParcelable(KEY_AUDIO_FILE, mAudioFile);
        outState.putInt(KEY_PROGRESS_VISIBILITY, mAudioProgressBar.getVisibility());
        super.onSaveInstanceState(outState);
    }

    private void restoreSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mBound = savedInstanceState.getBoolean(KEY_SERVICE_BOUND);
            mAudioFile = savedInstanceState.getParcelable(KEY_AUDIO_FILE);
            if (!broadcastUpdateFinished) {
                updatePlayerUI();
            }
        }
    }

    @Override
    public void onRefresh() {
        loadData(true);
    }

    public void updateAdapter(List<AudioFile> data) {
        if (mAudioFile != null) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getId() == mAudioFile.getId()) {
                    data.get(i).setState(PlayerState.PLAY);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void loadData(boolean pullToRefresh) {
        if (pullToRefresh) {
            presenter.getModel().getRequestParams().setPage(1);
            mScrollListener.resetState();
            presenter.clear();
        }
        presenter.loadData(pullToRefresh, presenter.getModel().getRequestParams());
    }
    //--------------------------Player UI----------------------------------------//
    private void setupPlayerCoverListener() {
        audioCoverImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getPlayingPosition() != -1) {
                    PlayerState audioState = mAudioFile.getState();
                    int icon;
                    PlayerState state;
                    if (audioState == PlayerState.PLAY) {
                        state = PlayerState.PAUSE;
                        icon = R.drawable.ic_play_white;
                        pausePlayerMessage();
                    } else {
                        state = PlayerState.PLAY;
                        icon = R.drawable.ic_pause_white;
                        continuePlayingMessage();
                    }
                    mAdapter.setPlayerState(state);
                    audioPlayPause.setImageDrawable(ContextCompat.getDrawable(AudioListActivity.this, icon));
                    if (audioState == PlayerState.STOP) {
                        mDarkLayer.setVisibility(View.GONE);
                        audioPlayPause.setVisibility(View.VISIBLE);
                    } else {
                        mDarkLayer.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void updatePlayerUI() {
        if (mAudioFile != null) {
            audioTitle.setText(mAudioFile.getTitle());
            audioSeek.setMax(mAudioFile.getDurationInSeconds());
            if (mAudioFile.getImageBitmap() != null) {
                audioCoverImage.setImageBitmap(mAudioFile.getImageBitmap());
            }
            int icon = R.drawable.ic_pause_white;
            if (mAudioFile.getState() == PlayerState.PLAY && !mAudioFile.isLoading()) {
                hideAudioLoading();
                audioPlayPause.setVisibility(View.VISIBLE);
            } else if (mAudioFile.getState() == PlayerState.PAUSE && !mAudioFile.isLoading()) {
                icon = R.drawable.ic_play_white;
                hideAudioLoading();
                audioPlayPause.setVisibility(View.VISIBLE);
            } else {
                showAudioLoading();
                audioPlayPause.setVisibility(View.GONE);
            }
            mDarkLayer.setVisibility(View.VISIBLE);
            audioPlayPause.setImageDrawable(ContextCompat.getDrawable(this, icon));
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public void startPlaying(AudioFile item) {
        presenter.loadSubtitles(new SubtitlesRequestParams(item.getId()));
        startBufferingMessage(item);
    }

    public void continuePlaying() {
        continuePlayingMessage();
    }

    public void pausePlayer() {
        pausePlayerMessage();
    }

    private void hideAudioLoading() {
        mAudioProgressBar.setVisibility(View.GONE);
    }

    private void showAudioLoading() {
        mAudioProgressBar.setVisibility(View.VISIBLE);
    }

    public void startPlayerMessage() {
        presenter.sendMessage(null, PlayerService.MESSAGE_PLAY);
    }

    public void startBufferingMessage(AudioFile audioFile) {
        Glide.with(this)
                .load(audioFile.getImageFileUrl())
                .asBitmap()
                .priority(Priority.HIGH).listener(new RequestListener<String, Bitmap>() {
            @Override
            public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                mAudioFile.setImageBitmap(resource);
                audioCoverImage.setImageBitmap(resource);
                return false;
            }
        }).into(audioCoverImage);
        audioSubtitles.setText(getString(R.string.dash));
        mAudioFile = audioFile;
        mAudioFile.setLoading(true);
        updatePlayerUI();
        bindPlayerService();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_AUDIO_URL, audioFile.getAudioFileUrl());
        presenter.sendMessage(bundle, PlayerService.MESSAGE_START_BUFFERING);
    }

    public void continuePlayingMessage() {
        updatePlayerUI();
        if (mAudioFile != null)
            mAudioFile.setState(PlayerState.PLAY);
        presenter.sendMessage(null, PlayerService.MESSAGE_CONTINUE);
    }

    public void pausePlayerMessage() {
        if (mAudioFile != null)
            mAudioFile.setState(PlayerState.PAUSE);
        updatePlayerUI();
        presenter.sendMessage(null, PlayerService.MESSAGE_PAUSE);
    }

    public void updateButtonsVisibility() {
        mLengthButton.setText(getString(R.string.length));
        mCategoryButton.setText(getString(R.string.categories));
    }

//--------------------------Player UI----------------------------------------//
//-----------------------Lifecycle Methods-----------------------------------//

    @Override
    public void onResume() {
        if(modelHasData()){
            updateButtonsVisibility();
        }
        if (mAudioFile != null) {
            if (mAudioFile.isLoading()) {
                mAudioFile.setLoading(false);
                mAudioFile.setState(PlayerState.PLAY);
                updatePlayerUI();
            }
        }
        mPlayerBroadcast = new AudioReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DID_NOT_STAR);
        intentFilter.addAction(ACTION_AUDIO_STATE);
        registerReceiver(mPlayerBroadcast, intentFilter);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mPlayerBroadcast != null) {
            unregisterReceiver(mPlayerBroadcast);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            unbindService(presenter.playerConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindPlayerService();
    }

//--------------------------Lifecycle Methods--------------------------------//
//---------------------------------------------------------------------------//
    public void handleSubtitleMessage(Message message) {
        long time = (long) message.obj * 1000;
        if (presenter.getSubtitleEngine().size() > 0)
            audioSubtitles.setText(presenter.getSubtitleEngine().updateSubtitles(time).getTextEn());
    }

    public void handlePlaybackTimeMessage(Message message) {
        long time = (long) message.obj / 1000;
        audioSeek.setProgress((int) time);
        audioPlayed.setText(FacadeCommon.getDateFromMillis((Long) message.obj));
        long duration = mAudioFile.getDurationInSeconds();
        audioLeft.setText(String.format(getString(R.string.leftTime), FacadeCommon.getDateFromMillis(duration * 1000 - (long) message.obj)));
    }

    public void bindPlayerService() {
        if (!mBound) {
            Intent intent = new Intent(this, PlayerService.class);
            Messenger messenger = new Messenger(presenter.playbackHandler);
            intent.putExtra(BINDER_MESSENGER, messenger);
            bindService(intent, presenter.playerConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void setRefreshing(boolean refreshing) {
        swipeContainer.setRefreshing(refreshing);
    }

    //Broadcast получаемый при начале воспроизведения аудио
    private class AudioReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_AUDIO_STATE)) {
                mAudioFile.setLoading(intent.getBooleanExtra(KEY_PLAYER_STATE, false));
                updatePlayerUI();
                broadcastUpdateFinished = true;
            } else if (intent.getAction().equals(ACTION_DID_NOT_STAR)) {
                hideAudioLoading();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class EndlessScrollListener extends EndlessRecyclerViewScrollListener {

        public EndlessScrollListener(LinearLayoutManager layoutManager) {
            super(layoutManager);
        }

        @Override
        public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
            if(totalItemsCount>14){
                presenter.getModel().getRequestParams().setPage(presenter.getModel().getRequestParams().getPage() + 1);
                loadData(false);
            }
        }
    }

    public void onNext(AudioData value) {
        if (value.getPrimaryData().size() == 0 && presenter.getModel().getRequestParams().getPage() == 1) {
            showNoContentView();
        } else {
            hideNoContentView();
            mAdapter.setPlayingPosition(-1);
        }
        updateAdapter(value.getPrimaryData());
    }

    public void onError(Throwable e) {
        hideProgress();
        if (presenter.getModel().getItems() == null)
            getNoContentFoundLayout().setVisibility(View.VISIBLE);
        updateAdapter(new ArrayList<>());
        e.printStackTrace();
    }

    @Override
    public Context getAppContext() {
        return getApplicationContext();
    }

    @Override
    public Context getActivityContext() {
        return this;
    }
}
