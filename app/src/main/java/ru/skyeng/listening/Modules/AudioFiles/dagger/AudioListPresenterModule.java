package ru.skyeng.listening.Modules.AudioFiles.dagger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ru.skyeng.listening.CommonComponents.ServiceGenerator;
import ru.skyeng.listening.Modules.AudioFiles.AudioListModel;
import ru.skyeng.listening.Modules.AudioFiles.AudioListPresenter;
import ru.skyeng.listening.Modules.AudioFiles.SubtitlesModel;
import ru.skyeng.listening.Modules.AudioFiles.model.SubtitleEngine;
import ru.skyeng.listening.Modules.AudioFiles.network.AudioFilesService;
import ru.skyeng.listening.Modules.AudioFiles.network.SubtitlesService;

/**
 * ---------------------------------------------------
 * Created by Sermilion on 10/02/2017.
 * Project: Listening
 * ---------------------------------------------------
 * <a href="http://www.skyeng.ru">www.skyeng.ru</a>
 * <a href="http://www.github.com/sermilion>github</a>
 * ---------------------------------------------------
 */

@Module
class AudioListPresenterModule {

    @Provides
    @Singleton
    AudioListModel getAudioListModel(){
        return new AudioListModel();
    }

    @Provides
    @Singleton
    SubtitlesModel getSubtitlesModel(){
        return new SubtitlesModel();
    }

    @Provides
    SubtitleEngine getSubtitleEngine(){
        return new SubtitleEngine();
    }

}
