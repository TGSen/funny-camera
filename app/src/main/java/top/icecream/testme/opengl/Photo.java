package top.icecream.testme.opengl;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glDrawArrays;
import static top.icecream.testme.opengl.Constans.BYTES_PER_FLOAT;

/**
 * AUTHOR: 86417
 * DATE: 5/4/2017
 */

public class Photo {

    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private VertexArray vertexArray;

    private static float shapeCoords[] = {
            //order of coordinate:X,Y,S,T
            //Triangle fan
            0f, 0f, 0.5f, 0.5f,
            -1f, -1f, 0f, 0f,
            1f, -1f, 0f, 1f,
            1f, 1f, 1f, 1f,
            -1f, 1f, 1f, 0f,
            -1f, -1f, 0f, 0f
    };



    public Photo() {
        vertexArray = new VertexArray(shapeCoords);
    }

    public void bindData(GrayFilterRender grayFilterRender){
        vertexArray.setVertexAttribPointer(
                0,
                grayFilterRender.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE
        );

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                grayFilterRender.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE
        );

    }

    public void draw(){
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
    }

}
