package ru.skyeng.listening.CommonComponents;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import ru.skyeng.listening.AudioFiles.AudioListFragment;
import ru.skyeng.listening.CommonComponents.Interfaces.ActivityExtensions;
import ru.skyeng.listening.R;

public class BaseActivity extends AppCompatActivity implements ActivityExtensions {

    protected ProgressBar mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
    }

    protected Toolbar setupToolbar(String title, boolean home, int... homeAsUpIndicator) {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(title);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null && home) {
            if (homeAsUpIndicator.length > 0) {
                getSupportActionBar().setHomeAsUpIndicator(homeAsUpIndicator[0]);
            }
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        return mToolbar;
    }

    protected Fragment setupRecyclerFragment(Bundle inState, Class<? extends Fragment> fragmentClass, int containerId) {
        Fragment fragment;
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (inState != null) {
            fragment = manager.getFragment(inState, fragmentClass.getName());
        } else {
            fragment = FragmentFactory.createFragmentWithName(fragmentClass);
            transaction.add(containerId, fragment, fragmentClass.getName());
            transaction.commit();
        }
        return fragment;
    }

    public void showProgress(){
        mProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress(){
        mProgress.setVisibility(View.GONE);
    }
}