package ru.skyeng.listening.Modules.Categories;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.skyeng.listening.CommonComponents.BaseActivity;
import ru.skyeng.listening.CommonComponents.FilterSingleton;
import ru.skyeng.listening.CommonComponents.SEApplication;
import ru.skyeng.listening.CommonComponents.Interfaces.MVPBase.MVPView;
import ru.skyeng.listening.Modules.AudioFiles.AudioListActivity;
import ru.skyeng.listening.Modules.Categories.model.AudioTag;
import ru.skyeng.listening.R;
import ru.skyeng.listening.Utility.TagView.Tag;
import ru.skyeng.listening.Utility.TagView.TagView;

import static ru.skyeng.listening.Modules.AudioFiles.AudioListActivity.TAG_REQUEST_DATA;

public class CategoriesActivity extends BaseActivity<MVPView, CategoriesPresenter> implements View.OnClickListener, MVPView{

    @BindView(R.id.button_reset)
    Button mResetTagsButton;
    @BindView(R.id.button_apply)
    Button mApplyTagsButton;
    @BindView(R.id.tag_group)
    TagView tagGroup;
    private List<Integer> selectedTags;

    @Inject
    public void setPresenter(@NonNull CategoriesPresenter presenter) {
        super.setPresenter(presenter);
    }

    public void setSelectedTags(List<Integer> selectedTags) {
        this.selectedTags = selectedTags;
    }

    public List<Integer> getSelectedTags() {
        return selectedTags;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SEApplication.getINSTANCE().getCategoriesDiComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        ButterKnife.bind(this);
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        setupToolbar(getString(R.string.select_categories), R.drawable.ic_x);
        Gson gson = new Gson();
        Type type = new TypeToken<List<Integer>>(){}.getType();
        setSelectedTags(gson.fromJson(getIntent().getStringExtra(TAG_REQUEST_DATA), type));
        FilterSingleton mFilter = FilterSingleton.getInstance();
        selectedTags = mFilter.getSelectedTags();
        tagGroup.setOnTagClickListener(new TagView.OnTagClickListener() {
            @Override
            public void onTagClick(Tag tag, int position) {
                if (selectedTags.contains(position)) {
                    selectedTags.remove(Integer.valueOf(position));
                } else {
                    selectedTags.add(position);
                }

                initTagView();
            }
        });
        mApplyTagsButton.setOnClickListener(this);
        mResetTagsButton.setOnClickListener(this);
        if(presenter.getData()==null){
            presenter.loadData(false);
        }
        initTagView();
    }

    public void initTagView() {
        if (selectedTags == null) {
            selectedTags = new ArrayList<>();
        } else if (selectedTags.size() > 0) {
            AudioListActivity.categoriesSelected = true;
        }
        if (presenter.getData() == null || tagGroup == null) return;
        tagGroup.removeAll();
        for (int i = 0; i < presenter.getData().size(); i++) {
            AudioTag aTag = presenter.getData().get(i);
            Tag tag = new Tag(aTag.getTitle());
            tag.radius = 6;

            if (selectedTags.contains(i)) {
                tag.background = ContextCompat.getDrawable(this, R.drawable.blue2_with_shadow);
                tag.tagTextColor = ContextCompat.getColor(this, R.color.colorWhite);
            } else {
                tag.layoutColor = ContextCompat.getColor(this, R.color.colorBlue0);
                tag.tagTextColor = ContextCompat.getColor(this, R.color.colorAccent);
            }
            tag.tagTextSize = 16;
            tag.typeface = Typeface.DEFAULT_BOLD;
            tagGroup.addTag(tag);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.button_apply:
                applyCategories();
                break;
            case R.id.button_reset:
                resetSelectedTags();
                setResult(Activity.RESULT_OK, new Intent());
                finish();
                break;
        }
    }

    private void applyCategories() {
        if(getSelectedTags().size()>0){
            Intent returnIntent = new Intent();
            Gson gson = new Gson();
            String jsonTags = gson.toJson(getSelectedTags());
            returnIntent.putExtra(TAG_REQUEST_DATA, jsonTags);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        } else {
            Toast.makeText(CategoriesActivity.this, getResources().getString(R.string.select_tags), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void resetSelectedTags() {
        selectedTags.clear();
        initTagView();
        AudioListActivity.categoriesSelected = false;
    }


    @Override
    public Context getAppContext() {
        return SEApplication.getINSTANCE();
    }

    @Override
    public Context getActivityContext() {
        return this;
    }
}
