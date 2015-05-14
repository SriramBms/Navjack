
package de.tudarmstadt.tk.carsensing.activity.view;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Font util class
 * 
 * @author chris
 */
public class FontUtil {

    public static final int ROBOTO_REGULAR = 0;
    public static final int ROBOTO_BOLD = 1;
    public static final int ROBOTO_ITALIC = 2;
    public static final int ROBOTO_BOLD_ITALIC = 3;
    public static final int ROBOTO_LIGHT = 4;
    public static final int ROBOTO_THIN = 5;
    public static final int ROBOTO_BOLD_CONDENSED = 6;

    /**
     * Get roboto font
     * 
     * @param style 0: regular, 1: bold, 2:italic, 3: bolditalic
     * @return
     */
    public static Typeface getRobotoFont(Context context, int style) {
        Typeface tf = null;
        switch (style) {
            case ROBOTO_REGULAR:
                tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
                break;
            case ROBOTO_BOLD:
                tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Bold.ttf");
                break;
            case ROBOTO_ITALIC:
                tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Italic.ttf");
                break;
            case ROBOTO_BOLD_ITALIC:
                tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-BoldItalic.ttf");
                break;
            case ROBOTO_LIGHT:
                tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
                break;
            case ROBOTO_THIN:
                tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
                break;
            case ROBOTO_BOLD_CONDENSED:
                tf = Typeface
                        .createFromAsset(context.getAssets(), "fonts/Roboto-BoldCondensed.ttf");
                break;

        }
        return tf;
    }
    
    public static Typeface getDigiFont(Context context, int style) {
    	Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/Digifont.ttf");
        
        return tf;
    }
    

    public static void setCustomTypeface(TextView textView, int typeFace) {
        Typeface tf = getRobotoFont(textView.getContext(), typeFace);
        textView.setTypeface(tf);
    }
    
    public static void setTypefaceDigifont(TextView textView, int typeFace){
    	Typeface tf = getDigiFont(textView.getContext(), typeFace);
        textView.setTypeface(tf);
    }

}
