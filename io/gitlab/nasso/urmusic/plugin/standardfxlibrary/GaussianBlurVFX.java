package io.gitlab.nasso.urmusic.plugin.standardfxlibrary;

import static com.jogamp.opengl.GL.*;

import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import io.gitlab.nasso.urmusic.model.project.VideoEffect;
import io.gitlab.nasso.urmusic.model.project.VideoEffectArgs;
import io.gitlab.nasso.urmusic.model.project.param.FloatParam;
import io.gitlab.nasso.urmusic.model.project.param.OptionParam;
import io.gitlab.nasso.urmusic.model.renderer.video.NGLUtils;

public class GaussianBlurVFX extends VideoEffect {
	private static final int POPTVAL_BOTH = 0;
	private static final int POPTVAL_HORIZONTAL = 1;
	private static final int POPTVAL_VERTICAL = 2;
	
	private static final String PNAME_direction = "direction";
	private static final String PNAME_radius = "radius";
	
	private static NGLUtils glu = new NGLUtils("gaussian blur global", GaussianBlurVFX.class.getClassLoader());

	private int prog, quadVAO;
	private int loc_sigma_radius_offsetScale, loc_inputTex;
	
	private class GaussianBlurVFXInstance extends VideoEffectInstance {
		private final IntBuffer bufTex = Buffers.newDirectIntBuffer(1);
		private final IntBuffer bufFbo = Buffers.newDirectIntBuffer(1);
		
		private int alt_fbo = 0;
		private int alt_fbo_color = 0;
		
		private int alt_fbo_width = 0;
		private int alt_fbo_height = 0;
		
		private float radius;
		private float sigma;
		
		public void setupParameters() {
			this.addParameter(new OptionParam(PNAME_direction, 0,
				"both",
				"horizontal",
				"vertical"
			));
			this.addParameter(new FloatParam(PNAME_radius, 8, 1, 0, 128));
		}

		public void setupVideo(GL3 gl) {
		}

		private void setupAltFBO(GL3 gl, VideoEffectArgs args) {
			if(this.alt_fbo_width == args.width && this.alt_fbo_height == args.height)
				return;
			
			if(this.alt_fbo == 0) {
				glu.createFramebuffers(gl, 1, args.width, args.height, this.bufTex, this.bufFbo);
				this.alt_fbo = this.bufFbo.get(0);
				this.alt_fbo_color = this.bufTex.get(0);
			} else {
				gl.glBindTexture(GL_TEXTURE_2D, this.alt_fbo_color);
				gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, args.width, args.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
			}
			
			this.alt_fbo_width = args.width;
			this.alt_fbo_height = args.height;
		}
		
		private void hPass(GL3 gl, VideoEffectArgs args) {
			gl.glUniform4f(GaussianBlurVFX.this.loc_sigma_radius_offsetScale, this.sigma, this.radius, 1.0f / args.width, 0.0f);
			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		}

		private void vPass(GL3 gl, VideoEffectArgs args) {
			gl.glUniform4f(GaussianBlurVFX.this.loc_sigma_radius_offsetScale, this.sigma, this.radius, 0.0f, 1.0f / args.height);
			gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
		}
		
		public void applyVideo(GL3 gl, VideoEffectArgs args) {
			int direction = (int) args.parameters.get(PNAME_direction);
			this.sigma = (float) args.parameters.get(PNAME_radius);
			
			if(this.sigma == 0) {
				args.cancelled = true;
				return;
			}
			
			this.radius = (float) Math.ceil(this.sigma * 3);
			
			gl.glUseProgram(GaussianBlurVFX.this.prog);
			
			gl.glBindVertexArray(GaussianBlurVFX.this.quadVAO);
			
			switch(direction) {
				case POPTVAL_BOTH:
					this.setupAltFBO(gl, args);
					
					gl.glBindFramebuffer(GL_FRAMEBUFFER, this.alt_fbo);
					GaussianBlurVFX.glu.uniformTexture(gl, GaussianBlurVFX.this.loc_inputTex, args.texInput, 0);
					this.hPass(gl, args);

					gl.glBindFramebuffer(GL_FRAMEBUFFER, args.fboOutput);
					GaussianBlurVFX.glu.uniformTexture(gl, GaussianBlurVFX.this.loc_inputTex, this.alt_fbo_color, 0);
					this.vPass(gl, args);
					break;
				case POPTVAL_HORIZONTAL:
					GaussianBlurVFX.glu.uniformTexture(gl, GaussianBlurVFX.this.loc_inputTex, args.texInput, 0);
					this.hPass(gl, args);
					break;
				case POPTVAL_VERTICAL:
					GaussianBlurVFX.glu.uniformTexture(gl, GaussianBlurVFX.this.loc_inputTex, args.texInput, 0);
					this.vPass(gl, args);
					break;
			}
		}

		public void disposeVideo(GL3 gl) {
		}
	}
	
	public VideoEffectInstance instance() {
		return new GaussianBlurVFXInstance();
	}

	public void effectMain() {
		
	}

	public String getEffectClassID() {
		return "gaussian_blur";
	}

	public void globalVideoSetup(GL3 gl) {
		this.prog = GaussianBlurVFX.glu.createProgram(gl, "fx/gaussian_blur/", "main_vert.vs", "main_frag.fs");

		this.loc_sigma_radius_offsetScale = gl.glGetUniformLocation(this.prog, "sigma_radius_offsetScale");
		this.loc_inputTex = gl.glGetUniformLocation(this.prog, "inputTex");
		
		this.quadVAO = GaussianBlurVFX.glu.createFullQuadVAO(gl);
	}

	public void globalVideoDispose(GL3 gl) {
		glu.dispose(gl);
	}
}
