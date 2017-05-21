package top.icecream.testme.opengl;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glUniform2f;
import static top.icecream.testme.opengl.Constans.BYTES_PER_FLOAT;

/**
 * AUTHOR: 86417
 * DATE: 5/3/2017
 */

public class Table {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private VertexArray vertexArray;

    private static final float[] VERTEX_DATA = {
            //order of coordinate:X,Y,S,T
            //Triangle fan
            0f, 0f, 0.5f, 0.5f,
            -0.2f, -0.2f, 0f, 1f,
            0.2f, -0.2f, 1f, 1f,
            0.2f, 0.2f, 1f, 0f,
            -0.2f, 0.2f, 0f, 0f,
            -0.2f, -0.2f, 0f, 1f
    };

    public Table() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void bindData(TextureShaderProgram textureShaderProgram){
        vertexArray.setVertexAttribPointer(
                0,
                textureShaderProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE
        );

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                textureShaderProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE
        );
    }

    public void setCoor(TextureShaderProgram textureShaderProgram, float x, float y){
        int uniformLocation = textureShaderProgram.getDeletaxyCoordinates();
        glUniform2f(uniformLocation, x, y);
    }

    public void draw(){
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
    }
}
