package com.momently.orchestrator.adapter.out.agent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momently.orchestrator.config.PhotoInfoPipelineProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalPhotoInfoProcessCommandExecutorTest {

    @Test
    @DisplayName("ProcessCommandExecutor는 정상 종료 프로세스를 실행할 수 있다")
    void executesSuccessfulProcess() throws Exception {
        PhotoInfoPipelineProperties properties = new PhotoInfoPipelineProperties(
            "bash",
            "unused",
            "unused",
            "unused",
            "unused",
            "unused",
            "unused",
            1,
            "ffmpeg",
            1.0,
            3,
            4.0,
            4,
            true,
            false
        );

        Class<?> executorClass = Class.forName(
            "com.momently.orchestrator.adapter.out.agent.LocalPhotoInfoPipelineAdapter$ProcessCommandExecutor"
        );
        Constructor<?> constructor = executorClass.getDeclaredConstructor(PhotoInfoPipelineProperties.class);
        constructor.setAccessible(true);
        Object executor = constructor.newInstance(properties);

        Method execute = executorClass.getDeclaredMethod("execute", List.class);
        execute.setAccessible(true);

        assertThatCode(() -> execute.invoke(executor, List.of("bash", "-lc", "exit 0"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ProcessCommandExecutor는 비정상 종료를 예외로 전파한다")
    void throwsWhenProcessFails() throws Exception {
        PhotoInfoPipelineProperties properties = new PhotoInfoPipelineProperties(
            "bash",
            "unused",
            "unused",
            "unused",
            "unused",
            "unused",
            "unused",
            1,
            "ffmpeg",
            1.0,
            3,
            4.0,
            4,
            true,
            false
        );

        Class<?> executorClass = Class.forName(
            "com.momently.orchestrator.adapter.out.agent.LocalPhotoInfoPipelineAdapter$ProcessCommandExecutor"
        );
        Constructor<?> constructor = executorClass.getDeclaredConstructor(PhotoInfoPipelineProperties.class);
        constructor.setAccessible(true);
        Object executor = constructor.newInstance(properties);

        Method execute = executorClass.getDeclaredMethod("execute", List.class);
        execute.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                execute.invoke(executor, List.of("bash", "-lc", "echo boom >&2; exit 2"));
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        })
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("exit code 2")
            .hasMessageContaining("boom");
    }
}
