package top.icecream.testme.opengl;

import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.glDrawArrays;
import static top.icecream.testme.opengl.Constans.BYTES_PER_FLOAT;

/**
 * AUTHOR: 86417
 * DATE: 5/3/2017
 */

public class Mallet {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private VertexArray vertexArray;

    private static final float[] VERTEX_DATA = {
            //order of coordinate:X,Y,R,G,B
            0f,-0.4f,0f,0f,1f,
            0f,0.4f,1f,0f,0f
    };

    public Mallet() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void bindData(ColorShaderProgram colorShaderProgram){
        vertexArray.setVertexAttribPointer(
                0,
                colorShaderProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE
        );

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                colorShaderProgram.getColorAttributeLocation(),
                COLOR_COMPONENT_COUNT,
                STRIDE
        );
    }

    public void draw(){
        glDrawArrays(GL_POINTS, 0, 2);
    }

}
