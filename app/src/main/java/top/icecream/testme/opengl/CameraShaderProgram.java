package top.icecream.testme.opengl;

import android.content.Context;

import top.icecream.testme.opengl.utils.ResourceReader;
import top.icecream.testme.opengl.utils.ShaderHelper;

import static android.opengl.GLES20.glUseProgram;

/**
 * AUTHOR: 86417
 * DATE: 5/4/2017
 */

public class CameraShaderProgram {
    protected static final String U_TEXTURE_UNIT = "sTexture";

    protected static final String A_POSITION = "aPosition";
    protected static final String A_TEXTURE_COORDINATES = "aTextureCoord";

    protected final int program;

    protected CameraShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        program = ShaderHelper.buildProgram(
                ResourceReader.readFile(context, vertexShaderResourceId),
                ResourceReader.readFile(context, fragmentShaderResourceId)
        );
    }

    public void useProgram() {
        glUseProgram(program);
    }
}
