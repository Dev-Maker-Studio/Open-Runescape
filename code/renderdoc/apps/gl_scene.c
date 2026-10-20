#define GL_GLEXT_PROTOTYPES
#include <GL/glx.h>
#include <GL/glext.h>
#include <X11/Xlib.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

static const char *vs = "#version 330 core\n"
    "layout(location=0) in vec2 p; layout(location=1) in vec3 c;\n"
    "uniform vec4 tint; uniform float scale; out vec4 v;\n"
    "void main(){ v = vec4(c,1.0)*tint; gl_Position = vec4(p*scale + vec2(float(gl_InstanceID)*0.5-0.25, 0.0), 0.0, 1.0); }\n";
static const char *fs = "#version 330 core\nin vec4 v; out vec4 o; void main(){ o = v; }\n";

static GLuint program(void) {
    GLuint s[2] = {glCreateShader(GL_VERTEX_SHADER), glCreateShader(GL_FRAGMENT_SHADER)}, p = glCreateProgram();
    const char *src[2] = {vs, fs};
    for (int i = 0; i < 2; i++) { glShaderSource(s[i], 1, &src[i], 0); glCompileShader(s[i]); glAttachShader(p, s[i]); }
    glLinkProgram(p);
    return p;
}

int main(int argc, char **argv) {
    int frames = argc > 1 ? atoi(argv[1]) : 600;
    Display *d = XOpenDisplay(0);
    int attr[] = {GLX_RGBA, GLX_DOUBLEBUFFER, GLX_DEPTH_SIZE, 24, None};
    XVisualInfo *vi = glXChooseVisual(d, 0, attr);
    XSetWindowAttributes wa = {.colormap = XCreateColormap(d, RootWindow(d, vi->screen), vi->visual, AllocNone)};
    Window w = XCreateWindow(d, RootWindow(d, vi->screen), 0, 0, 256, 256, 0, vi->depth, InputOutput, vi->visual, CWColormap, &wa);
    XMapWindow(d, w);
    int cfgn, cattr[] = {GLX_RENDER_TYPE, GLX_RGBA_BIT, GLX_DOUBLEBUFFER, 1, GLX_DEPTH_SIZE, 24, None};
    GLXFBConfig *cfg = glXChooseFBConfig(d, 0, cattr, &cfgn);
    int ctxa[] = {GLX_CONTEXT_MAJOR_VERSION_ARB, 3, GLX_CONTEXT_MINOR_VERSION_ARB, 3, GLX_CONTEXT_PROFILE_MASK_ARB, GLX_CONTEXT_CORE_PROFILE_BIT_ARB, None};
    GLXContext (*mk)(Display *, GLXFBConfig, GLXContext, Bool, const int *) = (void *)glXGetProcAddress((const GLubyte *)"glXCreateContextAttribsARB");
    GLXContext cx = mk(d, cfg[0], 0, True, ctxa);
    glXMakeCurrent(d, w, cx);
    GLuint pr = program(), vao, buf[3];
    float verts[] = {-0.5f, -0.5f, 1, 0, 0, 0.5f, -0.5f, 0, 1, 0, 0.0f, 0.5f, 0, 0, 1};
    float quad[] = {-0.25f, -0.25f, 1, 1, 0, 0.25f, -0.25f, 0, 1, 1, 0.25f, 0.25f, 1, 0, 1, -0.25f, 0.25f, 1, 1, 1};
    unsigned short idx[] = {0, 1, 2, 0, 2, 3};
    glGenVertexArrays(1, &vao);
    glBindVertexArray(vao);
    glGenBuffers(3, buf);
    for (int frame = 0; frame < frames; frame++) {
        glViewport(0, 0, 256, 256);
        glClearColor(0.1f, 0.2f, 0.3f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(pr);
        glUniform4f(glGetUniformLocation(pr, "tint"), 1, 1, 1, 1);
        glUniform1f(glGetUniformLocation(pr, "scale"), 1);
        glDisable(GL_BLEND);
        glBindBuffer(GL_ARRAY_BUFFER, buf[0]);
        glBufferData(GL_ARRAY_BUFFER, sizeof verts, verts, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 20, (void *)0);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 20, (void *)8);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glUniform4f(glGetUniformLocation(pr, "tint"), 1, 1, 1, 0.5f);
        glUniform1f(glGetUniformLocation(pr, "scale"), 1.5f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindBuffer(GL_ARRAY_BUFFER, buf[1]);
        glBufferData(GL_ARRAY_BUFFER, sizeof quad, quad, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 20, (void *)0);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 20, (void *)8);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buf[2]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof idx, idx, GL_STATIC_DRAW);
        glDrawElementsInstanced(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0, 2);
        glXSwapBuffers(d, w);
        usleep(16000);
    }
    return 0;
}
