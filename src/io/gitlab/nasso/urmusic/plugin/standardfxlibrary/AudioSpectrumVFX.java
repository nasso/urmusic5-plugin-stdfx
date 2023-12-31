/*******************************************************************************
 * urmusic - The Free and Open Source Music Visualizer Tool
 * Copyright (C) 2018  nasso (https://github.com/nasso)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * Contact "nasso": nassomails -at- gmail dot com
 ******************************************************************************/
package io.gitlab.nasso.urmusic.plugin.standardfxlibrary;

import static com.jogamp.opengl.GL.*;

import org.joml.Vector2fc;

import com.jogamp.opengl.GL3;

import io.gitlab.nasso.urmusic.common.BoolValue;
import io.gitlab.nasso.urmusic.common.MathUtils;
import io.gitlab.nasso.urmusic.common.RGBA32;
import io.gitlab.nasso.urmusic.model.UrmusicModel;
import io.gitlab.nasso.urmusic.model.project.VideoEffect;
import io.gitlab.nasso.urmusic.model.project.VideoEffectArgs;
import io.gitlab.nasso.urmusic.model.project.param.AngleParam;
import io.gitlab.nasso.urmusic.model.project.param.BooleanParam;
import io.gitlab.nasso.urmusic.model.project.param.FloatParam;
import io.gitlab.nasso.urmusic.model.project.param.IntParam;
import io.gitlab.nasso.urmusic.model.project.param.OptionParam;
import io.gitlab.nasso.urmusic.model.project.param.Point2DParam;
import io.gitlab.nasso.urmusic.model.project.param.RGBA32Param;
import io.gitlab.nasso.urmusic.model.renderer.audio.AudioRenderer;
import io.gitlab.nasso.urmusic.model.renderer.video.NGLUtils;
import io.gitlab.nasso.urmusic.model.renderer.video.glvg.GLVG;
import io.gitlab.nasso.urmusic.model.renderer.video.glvg.VGLineCap;

public class AudioSpectrumVFX extends VideoEffect {
	private static final String PNAME_color = "color";
	private static final String PNAME_mode = "mode";
	private static final String PNAME_faceMode = "faceMode";
	private static final String PNAME_lineCaps = "lineCaps";
	private static final String PNAME_angle = "angle";
	private static final String PNAME_zeroLast = "zeroLast";
	private static final String PNAME_polar = "polar";
	private static final String PNAME_count = "count";
	private static final String PNAME_size = "size";
	private static final String PNAME_startPoint = "startPoint";
	private static final String PNAME_endPoint = "endPoint";
	private static final String PNAME_millisOffset = "millisOffset";
	private static final String PNAME_duration = "duration";
	private static final String PNAME_minDecibel = "minDecibel";
	private static final String PNAME_maxDecibel = "maxDecibel";
	private static final String PNAME_minFreq = "minFreq";
	private static final String PNAME_maxFreq = "maxFreq";
	private static final String PNAME_minHeight = "minHeight";
	private static final String PNAME_height = "height";
	private static final String PNAME_exponent = "exponent";
	private static final String PNAME_blendingMode = "blendingMode";
	
	private class AudioSpectrumVFXInstance extends VideoEffectInstance {
		private VideoEffectArgs args;
		
		private RGBA32 color;
		private int mode;
		private int faceMode;
		private int lineCaps;
		private float angle;
		private boolean zeroLast;
		private boolean polar;
		private int count;
		private float size;
		private Vector2fc startPoint;
		private Vector2fc endPoint;
		private float millisOffset;
		private float duration;
		private float minDecibel;
		private float maxDecibel;
		private float minFreq;
		private float maxFreq;
		private float minHeight;
		private float height;
		private float exponent;
		private int blendingMode;
		
		private GLVG vg;
		private NGLUtils glu = new NGLUtils("audio spectrum instance glu", AudioSpectrumVFX.class.getClassLoader());
		
		private int gl_dest_fbo;
		private int gl_dest_fbo_tex;
		private int gl_vao_fullQuad;
		private int gl_prog_blend;
		private int gl_prog_blend_inputTex;
		private int gl_prog_blend_destTex;
		private int gl_prog_blend_blendingMode;
		
		private int dest_width = -1;
		private int dest_height = -1;
		
		private float[] audioData = new float[AudioRenderer.FFT_SIZE];
		private float[] xy = new float[2];
		
		public void setupParameters() {
			// That's where the order matters
			this.addParameter(new RGBA32Param(PNAME_color, 0xFFFFFFFF));
			this.addParameter(new OptionParam(PNAME_mode, 1, "outline", "lines", "fill", "dots"));
			this.addParameter(new OptionParam(PNAME_faceMode, 2, "faceA", "faceB", "both", "alternate"));
			this.addParameter(new OptionParam(PNAME_lineCaps, 0, "butt", "round", "square"));
			this.addParameter(new AngleParam(PNAME_angle));
			this.addParameter(new BooleanParam(PNAME_zeroLast, BoolValue.TRUE));
			this.addParameter(new BooleanParam(PNAME_polar, BoolValue.FALSE));
			this.addParameter(new IntParam(PNAME_count, 128, 1, 1, Integer.MAX_VALUE));
			this.addParameter(new FloatParam(PNAME_size, 2.0f, 1.0f, 0.0f, Float.MAX_VALUE));
			this.addParameter(new Point2DParam(PNAME_startPoint, -500, 150));
			this.addParameter(new Point2DParam(PNAME_endPoint, +500, 150));
			this.addParameter(new FloatParam(PNAME_millisOffset, 0.0f, 1.0f));
			this.addParameter(new FloatParam(PNAME_duration, 200.0f, 1.0f, Float.MIN_VALUE, Float.MAX_VALUE));
			this.addParameter(new FloatParam(PNAME_minDecibel, -50.0f, 1.0f));
			this.addParameter(new FloatParam(PNAME_maxDecibel, -20.0f, 1.0f));
			this.addParameter(new FloatParam(PNAME_minFreq, 0.0f, 10.0f, 0.0f, Float.MAX_VALUE));
			this.addParameter(new FloatParam(PNAME_maxFreq, 140.0f, 10.0f, 0.0f, Float.MAX_VALUE));
			this.addParameter(new FloatParam(PNAME_minHeight, 2.0f, 1.0f, 0.0f, Float.MAX_VALUE));
			this.addParameter(new FloatParam(PNAME_height, 200.0f, 1.0f));
			this.addParameter(new FloatParam(PNAME_exponent, 2.0f, 1.0f, 0.0f, Float.MAX_VALUE));
			this.addParameter(new OptionParam(PNAME_blendingMode, 8, 
				"srcOver",
				"dstOver",
				"srcIn",
				"dstIn",
				"srcOut",
				"dstOut",
				"srcAtop",
				"dstAtop",
				"copy",
				"add",
				"xor"
			));
		}
		
		public void setupVideo(GL3 gl) {
			this.vg = new GLVG(gl);
			
			this.gl_dest_fbo = this.glu.genFramebuffer(gl);
			this.gl_dest_fbo_tex = this.glu.genTexture(gl);
			
			this.gl_vao_fullQuad = this.glu.createFullQuadVAO(gl);
			
			this.gl_prog_blend = this.glu.createProgram(gl, "fx/audio_spectrum/", "main_vert.vs", "main_frag.fs");
			this.gl_prog_blend_inputTex = gl.glGetUniformLocation(this.gl_prog_blend, "inputTex");
			this.gl_prog_blend_destTex = gl.glGetUniformLocation(this.gl_prog_blend, "destTex");
			this.gl_prog_blend_blendingMode = gl.glGetUniformLocation(this.gl_prog_blend, "blendingMode");
		}
		
		private void xyToPolar() {
			float a = this.xy[0] / this.args.width * MathUtils.PI_2 + MathUtils.HALF_PI;
			float r = this.xy[1];
			
			this.xy[0] = MathUtils.cosf(a) * r;
			this.xy[1] = MathUtils.sinf(a) * r;
		}
		
		private void traceFace(float expandX, float expandY, float minExpandX, float minExpandY, boolean alternate) {
			this.xy[0] = this.startPoint.x();
			this.xy[1] = -this.startPoint.y();
			if(this.polar) this.xyToPolar();
			
			if(this.zeroLast)
				this.vg.moveTo(this.xy[0], this.xy[1]);
			
			for(int i = 0; i < this.count; i++) {
				float p = (float) i / (this.count - 1);
				
				// [0..1] frequency amplitude
				float freqVal = MathUtils.clamp(Math.max((MathUtils.getValue(this.audioData, MathUtils.lerp(this.minFreq, this.maxFreq, p) * this.audioData.length, true) - this.minDecibel), 0.0f) / (this.maxDecibel - this.minDecibel), 0.0f, 1.0f);
				freqVal = MathUtils.powf(freqVal, this.exponent);
				
				this.xy[0] = MathUtils.lerp(this.startPoint.x(), this.endPoint.x(), p) + freqVal * expandX + minExpandX;
				this.xy[1] = -MathUtils.lerp(this.startPoint.y(), this.endPoint.y(), p) + freqVal * expandY + minExpandY;
				
				if(this.polar) this.xyToPolar();
				
				if(this.zeroLast || i != 0) this.vg.lineTo(this.xy[0], this.xy[1]);
				else this.vg.moveTo(this.xy[0], this.xy[1]);
				
				if(alternate) {
					expandX = -expandX;
					expandY = -expandY;
					minExpandX = -minExpandX;
					minExpandY = -minExpandY;
				}
			}
			
			if(this.zeroLast) {
				this.xy[0] = this.endPoint.x();
				this.xy[1] = -this.endPoint.y();
				if(this.polar) this.xyToPolar();
				
				this.vg.lineTo(this.xy[0], this.xy[1]);
			}
		}
		
		private void traceOutline() {
			float dx = this.endPoint.x() - this.startPoint.x();
			float dy = this.endPoint.y() - this.startPoint.y();
			float distInv = (float) (1.0 / Math.sqrt(dx * dx + dy * dy));
			float cosang = MathUtils.cosf(-this.angle);
			float sinang = MathUtils.sinf(-this.angle);
			float expandX = (cosang * dy + sinang * dx) * distInv * this.height;
			float expandY = (sinang * dy - cosang * dx) * distInv * this.height;
			float minExpandX = (cosang * dy + sinang * dx) * distInv * this.minHeight;
			float minExpandY = (sinang * dy - cosang * dx) * distInv * this.minHeight;
			
			this.vg.beginPath();
			
			// The trick to get different faces is to play with the sign
			switch(this.faceMode) {
				case 0: // faceA
					this.traceFace(-expandX, +expandY, -minExpandX, +minExpandY, false);
					break;
				case 1: // faceB
					this.traceFace(+expandX, -expandY, +minExpandX, -minExpandY, false);
					break;
				case 2: // both
					this.traceFace(-expandX, +expandY, -minExpandX, +minExpandY, false);
					this.traceFace(+expandX, -expandY, +minExpandX, -minExpandY, false);
					break;
				case 3: // alternate
					this.traceFace(-expandX, +expandY, -minExpandX, +minExpandY, true);
					break;
			}
		}
		
		private void renderFill() {
			this.traceOutline();
			this.vg.fill();
		}
		
		private void renderOutline() {
			this.traceOutline();
			this.vg.stroke();
		}
		
		private void renderLines() {
			float dx = this.endPoint.x() - this.startPoint.x();
			float dy = this.endPoint.y() - this.startPoint.y();
			float distInv = (float) (1.0 / Math.sqrt(dx * dx + dy * dy));
			float cosang = MathUtils.cosf(-this.angle);
			float sinang = MathUtils.sinf(-this.angle);
			float expandX = (cosang * dy + sinang * dx) * distInv * this.height;
			float expandY = (sinang * dy - cosang * dx) * distInv * this.height;
			float minExpandX = (cosang * dy + sinang * dx) * distInv * this.minHeight;
			float minExpandY = (sinang * dy - cosang * dx) * distInv * this.minHeight;
			
			boolean faceA = true;
			boolean faceB = false;
			
			switch(this.faceMode) {
				case 0: // faceA
					faceA = true;
					faceB = false;
					break;
				case 1: // faceB
					faceA = false;
					faceB = true;
					break;
				case 2: // both
					faceA = true;
					faceB = true;
					break;
			}
			
			this.vg.beginPath();
			for(int i = 0; i < this.count; i++) {
				float p = (float) i / (this.count - 1);
				
				// [0..1] frequency amplitude
				float freqVal = MathUtils.clamp(Math.max((MathUtils.getValue(this.audioData, MathUtils.lerp(this.minFreq, this.maxFreq, p) * this.audioData.length, true) - this.minDecibel), 0.0f) / (this.maxDecibel - this.minDecibel), 0.0f, 1.0f);
				freqVal = MathUtils.powf(freqVal, this.exponent);
				
				float sx = MathUtils.lerp(this.startPoint.x(), this.endPoint.x(), p);
				float sy = -MathUtils.lerp(this.startPoint.y(), this.endPoint.y(), p);
				float xa = sx - freqVal * expandX - minExpandX;
				float ya = sy + freqVal * expandY + minExpandY;
				float xb = sx + freqVal * expandX + minExpandX;
				float yb = sy - freqVal * expandY - minExpandY;

				if(faceA && faceB) {
					this.xy[0] = xa;
					this.xy[1] = ya;
					if(this.polar) this.xyToPolar();
					this.vg.moveTo(this.xy[0], this.xy[1]);
					
					this.xy[0] = xb;
					this.xy[1] = yb;
					if(this.polar) this.xyToPolar();
					this.vg.lineTo(this.xy[0], this.xy[1]);
				} else if(faceA) {
					this.xy[0] = sx;
					this.xy[1] = sy;
					if(this.polar) this.xyToPolar();
					this.vg.moveTo(this.xy[0], this.xy[1]);
					
					this.xy[0] = xa;
					this.xy[1] = ya;
					if(this.polar) this.xyToPolar();
					this.vg.lineTo(this.xy[0], this.xy[1]);
				} else if(faceB) {
					this.xy[0] = sx;
					this.xy[1] = sy;
					if(this.polar) this.xyToPolar();
					this.vg.moveTo(this.xy[0], this.xy[1]);

					this.xy[0] = xb;
					this.xy[1] = yb;
					if(this.polar) this.xyToPolar();
					this.vg.lineTo(this.xy[0], this.xy[1]);
				}
				
				if(this.faceMode == 3) { // alternate
					faceA = !faceA;
					faceB = !faceB;
				}

			}
			this.vg.stroke();
		}
		
		private void renderDots() {
			float dx = this.endPoint.x() - this.startPoint.x();
			float dy = this.endPoint.y() - this.startPoint.y();
			float distInv = (float) (1.0 / Math.sqrt(dx * dx + dy * dy));
			float cosang = MathUtils.cosf(-this.angle);
			float sinang = MathUtils.sinf(-this.angle);
			float expandX = (cosang * dy + sinang * dx) * distInv * this.height;
			float expandY = (sinang * dy - cosang * dx) * distInv * this.height;
			float minExpandX = (cosang * dy + sinang * dx) * distInv * this.minHeight;
			float minExpandY = (sinang * dy - cosang * dx) * distInv * this.minHeight;
			
			boolean faceA = true;
			boolean faceB = false;

			switch(this.faceMode) {
				case 0: // faceA
					faceA = true;
					faceB = false;
					break;
				case 1: // faceB
					faceA = false;
					faceB = true;
					break;
				case 2: // both
					faceA = true;
					faceB = true;
					break;
			}
			
			this.vg.beginPath();
			for(int i = 0; i < this.count; i++) {
				float p = (float) i / (this.count - 1);
				
				// [0..1] frequency amplitude
				float freqVal = MathUtils.clamp(Math.max((MathUtils.getValue(this.audioData, MathUtils.lerp(this.minFreq, this.maxFreq, p) * this.audioData.length, true) - this.minDecibel), 0.0f) / (this.maxDecibel - this.minDecibel), 0.0f, 1.0f);
				freqVal = MathUtils.powf(freqVal, this.exponent);
				
				float sx = MathUtils.lerp(this.startPoint.x(), this.endPoint.x(), p);
				float sy = -MathUtils.lerp(this.startPoint.y(), this.endPoint.y(), p);
				float xa = sx - freqVal * expandX - minExpandX;
				float ya = sy + freqVal * expandY + minExpandY;
				float xb = sx + freqVal * expandX + minExpandX;
				float yb = sy - freqVal * expandY - minExpandY;

				if(faceA) {
					this.xy[0] = xa;
					this.xy[1] = ya;
					if(this.polar) this.xyToPolar();
					this.vg.moveTo(this.xy[0], this.xy[1]);
					this.vg.oval(this.xy[0], this.xy[1], this.size, this.size);
				}
				
				if(faceB) {
					this.xy[0] = xb;
					this.xy[1] = yb;
					if(this.polar) this.xyToPolar();
					this.vg.moveTo(this.xy[0], this.xy[1]);
					this.vg.oval(this.xy[0], this.xy[1], this.size, this.size);
				}
				
				if(this.faceMode == 3) { // alternate
					faceA = !faceA;
					faceB = !faceB;
				}
			}
			this.vg.fill();
		}
		
		public void applyVideo(GL3 gl, VideoEffectArgs args) {
			this.args = args;
			this.color = ((RGBA32) args.parameters.get(PNAME_color));
			this.mode = ((int) args.parameters.get(PNAME_mode));
			this.faceMode = ((int) args.parameters.get(PNAME_faceMode));
			this.lineCaps = ((int) args.parameters.get(PNAME_lineCaps));
			this.angle = (float) Math.toRadians((float) args.parameters.get(PNAME_angle));
			this.zeroLast = args.parameters.get(PNAME_zeroLast) == BoolValue.TRUE;
			this.polar = args.parameters.get(PNAME_polar) == BoolValue.TRUE;
			this.count = ((int) args.parameters.get(PNAME_count));
			this.size = ((float) args.parameters.get(PNAME_size));
			this.startPoint = ((Vector2fc) args.parameters.get(PNAME_startPoint));
			this.endPoint = ((Vector2fc) args.parameters.get(PNAME_endPoint));
			this.millisOffset = ((float) args.parameters.get(PNAME_millisOffset));
			this.duration = ((float) args.parameters.get(PNAME_duration));
			this.minDecibel = ((float) args.parameters.get(PNAME_minDecibel));
			this.maxDecibel = ((float) args.parameters.get(PNAME_maxDecibel));
			this.minFreq = ((float) args.parameters.get(PNAME_minFreq)) / UrmusicModel.getAudioRenderer().getSampleRate();
			this.maxFreq = ((float) args.parameters.get(PNAME_maxFreq)) / UrmusicModel.getAudioRenderer().getSampleRate();
			this.minHeight = ((float) args.parameters.get(PNAME_minHeight));
			this.height = ((float) args.parameters.get(PNAME_height));
			this.exponent = ((float) args.parameters.get(PNAME_exponent));
			this.blendingMode = ((int) args.parameters.get(PNAME_blendingMode));
			
			{ // In a block so "min" and "max" aren't annoying later if we need to name vars like that
				float min = Math.min(this.minDecibel, this.maxDecibel);
				float max = Math.max(this.minDecibel, this.maxDecibel);
				
				this.minDecibel = min;
				this.maxDecibel = max;
			}
			
			UrmusicModel.getAudioRenderer().getFreqData(args.time + this.millisOffset / 1000.0f, this.duration / 1000.0f, this.audioData);

			if(this.dest_width != args.width || this.dest_height != args.height) {
				gl.glBindTexture(GL_TEXTURE_2D, this.gl_dest_fbo_tex);
				gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, args.width, args.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
				
				if(this.dest_width == -1 || this.dest_height == -1) {
					gl.glBindFramebuffer(GL_FRAMEBUFFER, this.gl_dest_fbo);
					gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.gl_dest_fbo_tex, 0);
				}
				
				this.dest_width = args.width;
				this.dest_height = args.height;
				
				gl.glBindTexture(GL_TEXTURE_2D, 0);
			}
			
			this.vg.begin(gl, args.width, args.height);
			this.vg.setLineWidth(this.size);
			this.vg.setStrokeColor(this.color.getRGBA());
			this.vg.setFillColor(this.color.getRGBA());
			
			switch(this.lineCaps) {
				case 0: // BUTT
					this.vg.setLineCaps(VGLineCap.BUTT);
					break;
				case 1: // ROUND
					this.vg.setLineCaps(VGLineCap.ROUND);
					break;
				case 2: // SQUARE
					this.vg.setLineCaps(VGLineCap.SQUARE);
					break;
			}
			
			switch(this.mode) {
				case 0: // OUTLINE
					this.renderOutline();
					break;
				case 2: // FILL
					this.renderFill();
					break;
				case 1: // LINES
					this.renderLines();
					break;
				case 3: // DOTS
					this.renderDots();
					break;
			}
			
			// Render directly for "COPY" mode
			if(this.blendingMode == 8) this.vg.end(args.fboOutput);
			else {
				this.vg.end(this.gl_dest_fbo);
				
				gl.glBindFramebuffer(GL_FRAMEBUFFER, args.fboOutput);
				gl.glUseProgram(this.gl_prog_blend);
				this.glu.uniformTexture(gl, this.gl_prog_blend_inputTex, this.gl_dest_fbo_tex, 0);
				this.glu.uniformTexture(gl, this.gl_prog_blend_destTex, args.texInput, 1);
				gl.glUniform1i(this.gl_prog_blend_blendingMode, this.blendingMode);
				gl.glBindVertexArray(this.gl_vao_fullQuad);
				gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
			}
		}
		
		public void disposeVideo(GL3 gl) {
			this.vg.dispose(gl);
			this.glu.dispose(gl);
		}
	}
	
	public void globalVideoSetup(GL3 gl) {
	}

	public void globalVideoDispose(GL3 gl) {
	}

	public VideoEffectInstance instance() {
		return new AudioSpectrumVFXInstance();
	}

	public void effectMain() {
	}

	public String getEffectClassID() {
		return "audio_spectrum";
	}
}
