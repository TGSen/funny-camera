package top.icecream.testme.opengl;

import android.content.Context;

import top.icecream.testme.opengl.utils.ResourceReader;
import top.icecream.testme.opengl.utils.ShaderHelper;

import static android.opengl.GLES20.glUseProgram;

/**
 * AUTHOR: 86417
 * DATE: 5/3/2017
 */

public class ShaderProgram {
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";

    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        program = ShaderHelper.buildProgram(
                ResourceReader.readFile(context, vertexShaderResourceId),
                ResourceReader.readFile(context, fragmentShaderResourceId)
        );
    }

    public void useProgram() {
        glUseProgram(program);
    }
}
