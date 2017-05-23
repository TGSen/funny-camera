#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES sTexture;
varying vec2 vTextureCoordinates;



void main()
{
    vec2 TexSize = vec2(400.0, 400.0);
    vec2 mosaicSize = vec2(8.0, 8.0);
    vec2 intXY = vec2(vTextureCoordinates.x*TexSize.x, vTextureCoordinates.y*TexSize.y);
    vec2 XYMosaic = vec2(floor(intXY.x/mosaicSize.x)*mosaicSize.x, floor(intXY.y/mosaicSize.y)*mosaicSize.y);
    vec2 UVMosaic = vec2(XYMosaic.x/TexSize.x, XYMosaic.y/TexSize.y);
    vec4 color = texture2D(sTexture, UVMosaic);
    gl_FragColor = color;
}