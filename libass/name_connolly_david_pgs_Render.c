/*
 * name_connolly_david_pgs_Render.c
 *
 * Copyright 2008 David Connolly. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include "name_connolly_david_pgs_Render.h"
#include "ass.h"
#include "stdlib.h"

#include <string.h>

int loaded = 0;

ass_library_t* ass_library = NULL;
ass_renderer_t* ass_renderer = NULL;
ass_track_t* ass_track = NULL;

JNIEXPORT void JNICALL Java_name_connolly_david_pgs_Render_openSubtitle
(JNIEnv * env, jobject obj, jstring filename, jint x, jint y) 
{
	jboolean iscopy;
	char* subfile = (char*) ((*env)->GetStringUTFChars(env, filename, &iscopy));
	
	if (is_subtitle_open()) {
		throw_render_exception(env, "Error opening Subtitle (Subtitle Already Open)");
	}
	
	ass_library = ass_library_init();
	
	if (!ass_library) {
		throw_render_exception(env, "Error initialising ASS Library");
	}
	
	ass_renderer = ass_renderer_init(ass_library);
	
	if (!ass_renderer) {
		throw_render_exception(env, "Error initialising ASS Renderer");
	}
	
	ass_set_fonts_dir(ass_library, ".");
	ass_set_font_scale(ass_renderer, 1.);
	ass_set_fonts(ass_renderer, NULL, "Arial");
	ass_set_frame_size(ass_renderer, x, y);
	
	ass_track = ass_read_file(ass_library, subfile, "UTF-8");
	
	if (!ass_track) {
		throw_render_exception(env, "Error initialising ASS Track");
	}
	
	(*env)->ReleaseStringUTFChars(env, filename, subfile);
	
	return;
}

JNIEXPORT void JNICALL Java_name_connolly_david_pgs_Render_closeSubtitle
(JNIEnv * env, jobject obj) 
{
	if (is_subtitle_open()) {
		ass_free_track(ass_track);
		ass_renderer_done(ass_renderer);
		ass_library_done(ass_library);
		ass_track = NULL;
		ass_renderer = NULL;
		ass_library = NULL;
	} else {
		throw_render_exception(env, "Error subtitle already Closed or not opened)");
	}
	
	return;
}

JNIEXPORT jint JNICALL Java_name_connolly_david_pgs_Render_changeDetect
(JNIEnv * env, jobject obj, jlong timecode)
{
	int changeDetect;
	
	if (!is_subtitle_open()) {
		throw_render_exception(env, "Subtitle Not Open");
	}
	
	ass_render_frame(ass_renderer, ass_track, (long long)(timecode), &changeDetect);

	return (jint) changeDetect;
}

JNIEXPORT jint JNICALL Java_name_connolly_david_pgs_Render_getEventCount
(JNIEnv * env, jobject obj)
{
	if (!is_subtitle_open()) {
		throw_render_exception(env, "Subtitle Not Open");
	}
	
	return ass_track->n_events;
}

JNIEXPORT jobject JNICALL Java_name_connolly_david_pgs_Render_getEventTimecode
(JNIEnv * env, jobject obj, jint event)
{
	jclass timecodeClass;
	jmethodID cid;
	jobject result;
	timecodeClass = (*env)->FindClass(env, "name/connolly/david/pgs/Timecode");
	long long start;
	long long end;
	
	if (timecodeClass == NULL) {
		throw_render_exception(env, "Error loading SubtitleEvent class");
	}
	
	cid = (*env)->GetMethodID(env, timecodeClass,
							  "<init>", "(JJ)V");
	if (cid == NULL) {
		throw_render_exception(env, "Error loading SubtitleEvent constructor");
	}
	
	if (event >= ass_track->n_events || event < 0) {
		throw_render_exception(env, "Requested SubtitleEvent does not exist");
	}
	
	start = ass_track->events[event].Start;
	end = start + ass_track->events[event].Duration;
	result = (*env)->NewObject(env, timecodeClass, cid, start, end);
	(*env)->DeleteLocalRef(env, timecodeClass);
	
	return result;
}

JNIEXPORT void JNICALL Java_name_connolly_david_pgs_Render_render
(JNIEnv * env, jobject obj, jobject image, jlong timecode)
{
	jclass cls = (*env)->GetObjectClass(env, image);
	jmethodID getRGB = (*env)->GetMethodID(env, cls, "getRGB", "(II)I");
	jmethodID setRGB = (*env)->GetMethodID(env, cls, "setRGB", "(III)V");
	int changeDetect;
	
	if (cls == NULL) {
		throw_render_exception(env, "Error getting reference to BufferedImage");
	}
	
	if (getRGB == NULL) {
		throw_render_exception(env, "Error getting reference to BufferedImage getRGB()");
	}
	
	if (setRGB == NULL) {
		throw_render_exception(env, "Error getting reference to BufferedImage setRGB()");
	}

	ass_image_t *p_img = ass_render_frame(ass_renderer,
										  ass_track, (long long)(timecode), &changeDetect);
	
	while (p_img != NULL)
	{ 
		const int r = (p_img->color >> 24)&0xff; 
		const int g = (p_img->color >> 16)&0xff; 
		const int b = (p_img->color >>  8)&0xff; 
		const int a = (p_img->color) & 0xFF;
		
		int x, y; 
		
		for( y = 0; y < p_img->h; y++ ) 
		{ 
			for( x = 0; x < p_img->w; x++ ) 
			{ 
				const int alpha = p_img->bitmap[y*p_img->stride+x]; 
				const int an = (255 - a) * alpha / 255; 
				
				// TYPE_INT_ARGB
				//const int argb = (an << 24) + rgb;
				
				const int old_argb = (*env)->CallIntMethod(env, image, getRGB, x + p_img->dst_x, y + p_img->dst_y);
				int new_argb = 0;
				const int old_a = (old_argb >> 24) & 0xFF;
				const int old_r = (old_argb >> 16) & 0xFF;
				const int old_g = (old_argb >>  8) & 0xFF;
				const int old_b =  old_argb & 0xFF;
				new_argb += (255 - ( 255 - old_a ) * ( 255 - an ) / 255) << 24;
				new_argb += ((( old_r * (255-an) + r * an ) / 255) & 0xFF) << 16;
                		new_argb += ((( old_g * (255-an) + g * an ) / 255) & 0xFF) << 8;
                		new_argb += ((( old_b * (255-an) + b * an ) / 255) & 0xFF);
				
				(*env)->CallVoidMethod(env, image, setRGB, x + p_img->dst_x, y + p_img->dst_y, new_argb);
			}
		}

		p_img = p_img->next;
	}
	
	(*env)->DeleteLocalRef(env, cls);	
}

void throw_render_exception(JNIEnv *env, const char *msg) {
	jclass ex = (*env)->FindClass(env, "name/connolly/david/pgs/RenderException");
	
	if (ex != NULL) {
		(*env)->ThrowNew(env, ex, msg);
	}
	
	(*env)->DeleteLocalRef(env, ex);
}

int is_subtitle_open() {
	return (ass_track != NULL && ass_renderer != NULL && ass_library != NULL);
}
