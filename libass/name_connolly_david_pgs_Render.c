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

#include <stdbool.h>
#include <string.h>

int loaded = 0;

// Cache Section
JavaVM *jvm;
jclass render_cls;
jmethodID render_message_id;
jfieldID render_id;
jclass timecode_cls;
jmethodID timecode_constructor;
jclass buffered_image_cls;
jmethodID set_rgb_id;
jmethodID get_rgb_id;
jclass subtitle_event_cls;
jmethodID set_clip_id;

ASS_Library* ass_library = NULL;
ASS_Renderer* ass_renderer = NULL;
ASS_Track* ass_track = NULL;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env;
	jclass cls;
	
	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2)) {
		return JNI_ERR;
	}
	
	// Cache section
	jvm = vm;
	
	// Render
	cls = (*env)->FindClass(env, "name/connolly/david/pgs/Render");
	if (cls == NULL) {
		return JNI_ERR;
	}
	
	render_cls = (*env)->NewWeakGlobalRef(env, cls);
	
	if (render_cls == NULL) {
		return JNI_ERR;
	}
	
	render_id = (*env)->GetStaticFieldID(env, render_cls, "INSTANCE", "Lname/connolly/david/pgs/Render;");
	if (render_id == NULL) {
		return JNI_ERR;
	}
	
	render_message_id = (*env)->GetMethodID(env, render_cls, "renderMessage", "(Ljava/lang/String;)V");
	if (render_message_id == NULL) {
		return JNI_ERR;
	}
	
	(*env)->DeleteLocalRef(env, cls);
	
	// Timecode
	cls = (*env)->FindClass(env, "name/connolly/david/pgs/Timecode");
	
	if (cls == NULL) {
		return JNI_ERR;
	}
	
	timecode_cls = (*env)->NewWeakGlobalRef(env, cls);
	if (timecode_cls == NULL) {
		return JNI_ERR;
	}
	
	timecode_constructor = (*env)->GetMethodID(env, timecode_cls,
											   "<init>", "(JJ)V");
	if (timecode_constructor == NULL) {
		return JNI_ERR;
	}
	
	(*env)->DeleteLocalRef(env, cls);
	
	// Buffered Image
	cls = (*env)->FindClass(env, "java/awt/image/BufferedImage");
	if (cls == NULL) {
		return JNI_ERR;
	}
	
	buffered_image_cls = (*env)->NewWeakGlobalRef(env, cls);
	if (buffered_image_cls == NULL) {
		return JNI_ERR;
	}
	
	get_rgb_id = (*env)->GetMethodID(env, buffered_image_cls, "getRGB", "(II)I");
	if (get_rgb_id == NULL) {
		return JNI_ERR;
	}
	
	set_rgb_id = (*env)->GetMethodID(env, buffered_image_cls, "setRGB", "(III)V");
	if (set_rgb_id == NULL) {
		return JNI_ERR;
	}
	
	(*env)->DeleteLocalRef(env, cls);
	
	cls = (*env)->FindClass(env, "name/connolly/david/pgs/SubtitleEvent");
	if (cls == NULL) {
		return JNI_ERR;
	}
	
	subtitle_event_cls = (*env)->NewWeakGlobalRef(env, cls);
	if (subtitle_event_cls == NULL) {
		return JNI_ERR;
	}
	
	set_clip_id = (*env)->GetMethodID(env, subtitle_event_cls, "setClip", "(IIII)V");
	if (set_clip_id == NULL) {
		return JNI_ERR;
	}
	
	(*env)->DeleteLocalRef(env, cls);
		
	return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL 
JNI_OnUnload(JavaVM *jvm, void *reserved)
{
	JNIEnv *env;
	if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
		return;
	}
	
	(*env)->DeleteWeakGlobalRef(env, render_cls);
	(*env)->DeleteWeakGlobalRef(env, timecode_cls);
	(*env)->DeleteWeakGlobalRef(env, buffered_image_cls);
	(*env)->DeleteWeakGlobalRef(env, subtitle_event_cls);
	
	return;
}

void sendRenderMessage(char *msg) 
{
	JNIEnv *env;
	
	(*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
	
	if ((*env)->ExceptionCheck(env)) {
		return;
	}
	
	jobject render = (*env)->GetStaticObjectField(env, render_cls, render_id);
	
	(*env)->CallVoidMethod(env, render, render_message_id, (*env)->NewStringUTF(env, msg));
}

JNIEXPORT void JNICALL Java_name_connolly_david_pgs_Render_openSubtitle
(JNIEnv * env, jobject obj, jstring dirname, jstring filename, jint x, jint y) 
{
	jboolean iscopy;
	char* subdir = (char*) ((*env)->GetStringUTFChars(env, dirname, &iscopy));
	char* subfile = (char*) ((*env)->GetStringUTFChars(env, filename, &iscopy));
	
	if (is_subtitle_open()) {
		throw_render_exception(env, "Error opening Subtitle (Subtitle Already Open)");
	}
	
	ass_library = ass_library_init();
	
	if (!ass_library) {
		throw_render_exception(env, "Error initialising ASS Library");
	}
	
	ass_set_fonts_dir(ass_library, subdir);
	ass_renderer = ass_renderer_init(ass_library);
	
	if (!ass_renderer) {
		throw_render_exception(env, "Error initialising ASS Renderer");
	}

	ass_set_font_scale(ass_renderer, 1.);
	ass_set_fonts(ass_renderer, NULL, "Arial", 1, "", 1);
	/*ass_renderer_t *priv, const char *default_font,
	const char *default_family, int fc, const char *config,
	int update*/
	
	ass_set_frame_size(ass_renderer, x, y);
	
	ass_track = ass_read_file(ass_library, subfile, "UTF-8");
	
	if (!ass_track) {
		throw_render_exception(env, "Error initialising ASS Track");
	}
	
	(*env)->ReleaseStringUTFChars(env, filename, subdir);
	(*env)->ReleaseStringUTFChars(env, filename, subfile);
	
	fflush(stdout);
	
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
	
	fflush(stdout);
	
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
	jobject result;
	long long start;
	long long end;
	
	if (event >= ass_track->n_events || event < 0) {
		throw_render_exception(env, "Requested Timecode does not exist");
	}
	
	start = ass_track->events[event].Start;
	end = start + ass_track->events[event].Duration;
	printf("start: %lld duration: %lld \n", ass_track->events[event].Start, ass_track->events[event].Duration);
	
	result = (*env)->NewObject(env, timecode_cls, timecode_constructor, start, end);
	
	return result;
}

JNIEXPORT void JNICALL Java_name_connolly_david_pgs_Render_render
(JNIEnv * env, jobject obj, jobject event, jobject image, jlong timecode)
{
	int changeDetect;
	bool rendered = false;
	int minX = 1920; // smallest dst_x // offset x
	int maxX = 0; // largetest dst_x + x
	int minY = 1080; // smallest dst_y // offset y
	int maxY = 0; // largest dst_y + y
	ASS_Image *p_img = ass_render_frame(ass_renderer,
										  ass_track, (long long)(timecode), &changeDetect);
	
	if (p_img != NULL) {
		rendered = true;
	}
	
	while (p_img != NULL)
	{ 
		const int r = (p_img->color >> 24)&0xff; 
		const int g = (p_img->color >> 16)&0xff; 
		const int b = (p_img->color >>  8)&0xff; 
		const int a = (p_img->color) & 0xFF;
		
		if (p_img->dst_x < minX) {
			minX = p_img->dst_x;
		}
		
		if ((p_img->dst_x + p_img->w) > maxX) {
			maxX = (p_img->dst_x + p_img->w);
		}
		
		if (p_img->dst_y < minY) {
			minY = p_img->dst_y;
		}
		
		if ((p_img->dst_y + p_img->h) > maxY) {
			maxY = (p_img->dst_y + p_img->h);
		}
		
		int x, y; 
		
		for( y = 0; y < p_img->h; y++ ) 
		{ 
			for( x = 0; x < p_img->w; x++ ) 
			{ 
				const int alpha = p_img->bitmap[y*p_img->stride+x]; 
				const int an = (255 - a) * alpha / 255; 
				
				// TYPE_INT_ARGB
				//const int argb = (an << 24) + rgb;
				
				const int old_argb = (*env)->CallIntMethod(env, image, get_rgb_id, x + p_img->dst_x, y + p_img->dst_y);
				int new_argb = 0;
				const int old_a = (old_argb >> 24) & 0xFF;
				const int old_r = (old_argb >> 16) & 0xFF;
				const int old_g = (old_argb >>  8) & 0xFF;
				const int old_b =  old_argb & 0xFF;
				new_argb += (255 - ( 255 - old_a ) * ( 255 - an ) / 255) << 24;
				new_argb += ((( old_r * (255-an) + r * an ) / 255) & 0xFF) << 16;
                		new_argb += ((( old_g * (255-an) + g * an ) / 255) & 0xFF) << 8;
                		new_argb += ((( old_b * (255-an) + b * an ) / 255) & 0xFF);
				
				(*env)->CallVoidMethod(env, image, set_rgb_id, x + p_img->dst_x, y + p_img->dst_y, new_argb);
			}
		}

		p_img = p_img->next;
	}
	
	if (rendered) {
		(*env)->CallVoidMethod(env, event, set_clip_id, minX, minY, maxX, maxY);
	}
		
	fflush(stdout);
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
