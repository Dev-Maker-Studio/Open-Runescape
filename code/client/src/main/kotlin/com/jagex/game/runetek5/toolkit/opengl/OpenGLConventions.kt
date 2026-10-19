@file:JvmName("OpenGLConventions")

package com.jagex.game.runetek5.toolkit.opengl;

// Usage: import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
		/** AccumOp */
		const val GL_ACCUM = 256
		const val GL_LOAD = 257
		const val GL_RETURN = 258
		const val GL_MULT = 259
		const val GL_ADD = 260


		/** AlphaFunction */
		const val GL_NEVER = 512
		const val GL_LESS = 513
		const val GL_EQUAL = 514
		const val GL_LEQUAL = 515
		const val GL_GREATER = 516
		const val GL_NOTEQUAL = 517
		const val GL_GEQUAL = 518
		const val GL_ALWAYS = 519


		/** AttribMask */
		const val GL_CURRENT_BIT = 1
		const val GL_POINT_BIT = 2
		const val GL_LINE_BIT = 4
		const val GL_POLYGON_BIT = 8
		const val GL_POLYGON_STIPPLE_BIT = 16
		const val GL_PIXEL_MODE_BIT = 32
		const val GL_LIGHTING_BIT = 64
		const val GL_FOG_BIT = 128
		const val GL_DEPTH_BUFFER_BIT = 256
		const val GL_ACCUM_BUFFER_BIT = 512
		const val GL_STENCIL_BUFFER_BIT = 1024
		const val GL_VIEWPORT_BIT = 2048
		const val GL_TRANSFORM_BIT = 4096
		const val GL_ENABLE_BIT = 8192
		const val GL_COLOR_BUFFER_BIT = 16384
		const val GL_HINT_BIT = 32768
		const val GL_EVAL_BIT = 65536
		const val GL_LIST_BIT = 131072
		const val GL_TEXTURE_BIT = 262144
		const val GL_SCISSOR_BIT = 524288
		const val GL_ALL_ATTRIB_BITS = 1048575


		/** BeginMode */
		const val GL_POINTS = 0
		const val GL_LINES = 1
		const val GL_LINE_LOOP = 2
		const val GL_LINE_STRIP = 3
		const val GL_TRIANGLES = 4
		const val GL_TRIANGLE_STRIP = 5
		const val GL_TRIANGLE_FAN = 6
		const val GL_QUADS = 7
		const val GL_QUAD_STRIP = 8
		const val GL_POLYGON = 9


		/** BlendingFactorDest */
		const val GL_ZERO = 0
		const val GL_ONE = 1
		const val GL_SRC_COLOR = 768
		const val GL_ONE_MINUS_SRC_COLOR = 769
		const val GL_SRC_ALPHA = 770
		const val GL_ONE_MINUS_SRC_ALPHA = 771
		const val GL_DST_ALPHA = 772
		const val GL_ONE_MINUS_DST_ALPHA = 773


		/** BlendingFactorSrc */
		const val GL_DST_COLOR = 774
		const val GL_ONE_MINUS_DST_COLOR = 775
		const val GL_SRC_ALPHA_SATURATE = 776


		/** Boolean */
		const val GL_TRUE = 1
		const val GL_FALSE = 0


		/** ClipPlaneName */
		const val GL_CLIP_PLANE0 = 12288
		const val GL_CLIP_PLANE1 = 12289
		const val GL_CLIP_PLANE2 = 12290
		const val GL_CLIP_PLANE3 = 12291
		const val GL_CLIP_PLANE4 = 12292
		const val GL_CLIP_PLANE5 = 12293


		/** DataType */
		const val GL_BYTE = 5120
		const val GL_UNSIGNED_BYTE = 5121
		const val GL_SHORT = 5122
		const val GL_UNSIGNED_SHORT = 5123
		const val GL_INT = 5124
		const val GL_UNSIGNED_INT = 5125
		const val GL_FLOAT = 5126
		const val GL_2_BYTES = 5127
		const val GL_3_BYTES = 5128
		const val GL_4_BYTES = 5129
		const val GL_DOUBLE = 5130


		/** DrawBufferMode */
		const val GL_NONE = 0
		const val GL_FRONT_LEFT = 1024
		const val GL_FRONT_RIGHT = 1025
		const val GL_BACK_LEFT = 1026
		const val GL_BACK_RIGHT = 1027
		const val GL_FRONT = 1028
		const val GL_BACK = 1029
		const val GL_LEFT = 1030
		const val GL_RIGHT = 1031
		const val GL_FRONT_AND_BACK = 1032
		const val GL_AUX0 = 1033
		const val GL_AUX1 = 1034
		const val GL_AUX2 = 1035
		const val GL_AUX3 = 1036


		/** ErrorCode */
		const val GL_NO_ERROR = 0
		const val GL_INVALID_ENUM = 1280
		const val GL_INVALID_VALUE = 1281
		const val GL_INVALID_OPERATION = 1282
		const val GL_STACK_OVERFLOW = 1283
		const val GL_STACK_UNDERFLOW = 1284
		const val GL_OUT_OF_MEMORY = 1285


		/** FeedBackMode */
		const val GL_2D = 1536
		const val GL_3D = 1537
		const val GL_3D_COLOR = 1538
		const val GL_3D_COLOR_TEXTURE = 1539
		const val GL_4D_COLOR_TEXTURE = 1540


		/** FeedBackToken */
		const val GL_PASS_THROUGH_TOKEN = 1792
		const val GL_POINT_TOKEN = 1793
		const val GL_LINE_TOKEN = 1794
		const val GL_POLYGON_TOKEN = 1795
		const val GL_BITMAP_TOKEN = 1796
		const val GL_DRAW_PIXEL_TOKEN = 1797
		const val GL_COPY_PIXEL_TOKEN = 1798
		const val GL_LINE_RESET_TOKEN = 1799


		/** FogMode */
		const val GL_EXP = 2048
		const val GL_EXP2 = 2049


		/** FrontFaceDirection */
		const val GL_CW = 2304
		const val GL_CCW = 2305


		/** GetMapTarget */
		const val GL_COEFF = 2560
		const val GL_ORDER = 2561
		const val GL_DOMAIN = 2562


		/** GetTarget */
		const val GL_CURRENT_COLOR = 2816
		const val GL_CURRENT_INDEX = 2817
		const val GL_CURRENT_NORMAL = 2818
		const val GL_CURRENT_TEXTURE_COORDS = 2819
		const val GL_CURRENT_RASTER_COLOR = 2820
		const val GL_CURRENT_RASTER_INDEX = 2821
		const val GL_CURRENT_RASTER_TEXTURE_COORDS = 2822
		const val GL_CURRENT_RASTER_POSITION = 2823
		const val GL_CURRENT_RASTER_POSITION_VALID = 2824
		const val GL_CURRENT_RASTER_DISTANCE = 2825
		const val GL_POINT_SMOOTH = 2832
		const val GL_POINT_SIZE = 2833
		const val GL_POINT_SIZE_RANGE = 2834
		const val GL_POINT_SIZE_GRANULARITY = 2835
		const val GL_LINE_SMOOTH = 2848
		const val GL_LINE_WIDTH = 2849
		const val GL_LINE_WIDTH_RANGE = 2850
		const val GL_LINE_WIDTH_GRANULARITY = 2851
		const val GL_LINE_STIPPLE = 2852
		const val GL_LINE_STIPPLE_PATTERN = 2853
		const val GL_LINE_STIPPLE_REPEAT = 2854
		const val GL_LIST_MODE = 2864
		const val GL_MAX_LIST_NESTING = 2865
		const val GL_LIST_BASE = 2866
		const val GL_LIST_INDEX = 2867
		const val GL_POLYGON_MODE = 2880
		const val GL_POLYGON_SMOOTH = 2881
		const val GL_POLYGON_STIPPLE = 2882
		const val GL_EDGE_FLAG = 2883
		const val GL_CULL_FACE = 2884
		const val GL_CULL_FACE_MODE = 2885
		const val GL_FRONT_FACE = 2886
		const val GL_LIGHTING = 2896
		const val GL_LIGHT_MODEL_LOCAL_VIEWER = 2897
		const val GL_LIGHT_MODEL_TWO_SIDE = 2898
		const val GL_LIGHT_MODEL_AMBIENT = 2899
		const val GL_SHADE_MODEL = 2900
		const val GL_COLOR_MATERIAL_FACE = 2901
		const val GL_COLOR_MATERIAL_PARAMETER = 2902
		const val GL_COLOR_MATERIAL = 2903
		const val GL_FOG = 2912
		const val GL_FOG_INDEX = 2913
		const val GL_FOG_DENSITY = 2914
		const val GL_FOG_START = 2915
		const val GL_FOG_END = 2916
		const val GL_FOG_MODE = 2917
		const val GL_FOG_COLOR = 2918
		const val GL_DEPTH_RANGE = 2928
		const val GL_DEPTH_TEST = 2929
		const val GL_DEPTH_WRITEMASK = 2930
		const val GL_DEPTH_CLEAR_VALUE = 2931
		const val GL_DEPTH_FUNC = 2932
		const val GL_ACCUM_CLEAR_VALUE = 2944
		const val GL_STENCIL_TEST = 2960
		const val GL_STENCIL_CLEAR_VALUE = 2961
		const val GL_STENCIL_FUNC = 2962
		const val GL_STENCIL_VALUE_MASK = 2963
		const val GL_STENCIL_FAIL = 2964
		const val GL_STENCIL_PASS_DEPTH_FAIL = 2965
		const val GL_STENCIL_PASS_DEPTH_PASS = 2966
		const val GL_STENCIL_REF = 2967
		const val GL_STENCIL_WRITEMASK = 2968
		const val GL_MATRIX_MODE = 2976
		const val GL_NORMALIZE = 2977
		const val GL_VIEWPORT = 2978
		const val GL_MODELVIEW_STACK_DEPTH = 2979
		const val GL_PROJECTION_STACK_DEPTH = 2980
		const val GL_TEXTURE_STACK_DEPTH = 2981
		const val GL_MODELVIEW_MATRIX = 2982
		const val GL_PROJECTION_MATRIX = 2983
		const val GL_TEXTURE_MATRIX = 2984
		const val GL_ATTRIB_STACK_DEPTH = 2992
		const val GL_CLIENT_ATTRIB_STACK_DEPTH = 2993
		const val GL_ALPHA_TEST = 3008
		const val GL_ALPHA_TEST_FUNC = 3009
		const val GL_ALPHA_TEST_REF = 3010
		const val GL_DITHER = 3024
		const val GL_BLEND_DST = 3040
		const val GL_BLEND_SRC = 3041
		const val GL_BLEND = 3042
		const val GL_LOGIC_OP_MODE = 3056
		const val GL_INDEX_LOGIC_OP = 3057
		const val GL_LOGIC_OP = 3057
		const val GL_COLOR_LOGIC_OP = 3058
		const val GL_AUX_BUFFERS = 3072
		const val GL_DRAW_BUFFER = 3073
		const val GL_READ_BUFFER = 3074
		const val GL_SCISSOR_BOX = 3088
		const val GL_SCISSOR_TEST = 3089
		const val GL_INDEX_CLEAR_VALUE = 3104
		const val GL_INDEX_WRITEMASK = 3105
		const val GL_COLOR_CLEAR_VALUE = 3106
		const val GL_COLOR_WRITEMASK = 3107
		const val GL_INDEX_MODE = 3120
		const val GL_RGBA_MODE = 3121
		const val GL_DOUBLEBUFFER = 3122
		const val GL_STEREO = 3123
		const val GL_RENDER_MODE = 3136
		const val GL_PERSPECTIVE_CORRECTION_HINT = 3152
		const val GL_POINT_SMOOTH_HINT = 3153
		const val GL_LINE_SMOOTH_HINT = 3154
		const val GL_POLYGON_SMOOTH_HINT = 3155
		const val GL_FOG_HINT = 3156
		const val GL_TEXTURE_GEN_S = 3168
		const val GL_TEXTURE_GEN_T = 3169
		const val GL_TEXTURE_GEN_R = 3170
		const val GL_TEXTURE_GEN_Q = 3171
		const val GL_PIXEL_MAP_I_TO_I = 3184
		const val GL_PIXEL_MAP_S_TO_S = 3185
		const val GL_PIXEL_MAP_I_TO_R = 3186
		const val GL_PIXEL_MAP_I_TO_G = 3187
		const val GL_PIXEL_MAP_I_TO_B = 3188
		const val GL_PIXEL_MAP_I_TO_A = 3189
		const val GL_PIXEL_MAP_R_TO_R = 3190
		const val GL_PIXEL_MAP_G_TO_G = 3191
		const val GL_PIXEL_MAP_B_TO_B = 3192
		const val GL_PIXEL_MAP_A_TO_A = 3193
		const val GL_PIXEL_MAP_I_TO_I_SIZE = 3248
		const val GL_PIXEL_MAP_S_TO_S_SIZE = 3249
		const val GL_PIXEL_MAP_I_TO_R_SIZE = 3250
		const val GL_PIXEL_MAP_I_TO_G_SIZE = 3251
		const val GL_PIXEL_MAP_I_TO_B_SIZE = 3252
		const val GL_PIXEL_MAP_I_TO_A_SIZE = 3253
		const val GL_PIXEL_MAP_R_TO_R_SIZE = 3254
		const val GL_PIXEL_MAP_G_TO_G_SIZE = 3255
		const val GL_PIXEL_MAP_B_TO_B_SIZE = 3256
		const val GL_PIXEL_MAP_A_TO_A_SIZE = 3257
		const val GL_UNPACK_SWAP_BYTES = 3312
		const val GL_UNPACK_LSB_FIRST = 3313
		const val GL_UNPACK_ROW_LENGTH = 3314
		const val GL_UNPACK_SKIP_ROWS = 3315
		const val GL_UNPACK_SKIP_PIXELS = 3316
		const val GL_UNPACK_ALIGNMENT = 3317
		const val GL_PACK_SWAP_BYTES = 3328
		const val GL_PACK_LSB_FIRST = 3329
		const val GL_PACK_ROW_LENGTH = 3330
		const val GL_PACK_SKIP_ROWS = 3331
		const val GL_PACK_SKIP_PIXELS = 3332
		const val GL_PACK_ALIGNMENT = 3333
		const val GL_MAP_COLOR = 3344
		const val GL_MAP_STENCIL = 3345
		const val GL_INDEX_SHIFT = 3346
		const val GL_INDEX_OFFSET = 3347
		const val GL_RED_SCALE = 3348
		const val GL_RED_BIAS = 3349
		const val GL_ZOOM_X = 3350
		const val GL_ZOOM_Y = 3351
		const val GL_GREEN_SCALE = 3352
		const val GL_GREEN_BIAS = 3353
		const val GL_BLUE_SCALE = 3354
		const val GL_BLUE_BIAS = 3355
		const val GL_ALPHA_SCALE = 3356
		const val GL_ALPHA_BIAS = 3357
		const val GL_DEPTH_SCALE = 3358
		const val GL_DEPTH_BIAS = 3359
		const val GL_MAX_EVAL_ORDER = 3376
		const val GL_MAX_LIGHTS = 3377
		const val GL_MAX_CLIP_PLANES = 3378
		const val GL_MAX_TEXTURE_SIZE = 3379
		const val GL_MAX_PIXEL_MAP_TABLE = 3380
		const val GL_MAX_ATTRIB_STACK_DEPTH = 3381
		const val GL_MAX_MODELVIEW_STACK_DEPTH = 3382
		const val GL_MAX_NAME_STACK_DEPTH = 3383
		const val GL_MAX_PROJECTION_STACK_DEPTH = 3384
		const val GL_MAX_TEXTURE_STACK_DEPTH = 3385
		const val GL_MAX_VIEWPORT_DIMS = 3386
		const val GL_MAX_CLIENT_ATTRIB_STACK_DEPTH = 3387
		const val GL_SUBPIXEL_BITS = 3408
		const val GL_INDEX_BITS = 3409
		const val GL_RED_BITS = 3410
		const val GL_GREEN_BITS = 3411
		const val GL_BLUE_BITS = 3412
		const val GL_ALPHA_BITS = 3413
		const val GL_DEPTH_BITS = 3414
		const val GL_STENCIL_BITS = 3415
		const val GL_ACCUM_RED_BITS = 3416
		const val GL_ACCUM_GREEN_BITS = 3417
		const val GL_ACCUM_BLUE_BITS = 3418
		const val GL_ACCUM_ALPHA_BITS = 3419
		const val GL_NAME_STACK_DEPTH = 3440
		const val GL_AUTO_NORMAL = 3456
		const val GL_MAP1_COLOR_4 = 3472
		const val GL_MAP1_INDEX = 3473
		const val GL_MAP1_NORMAL = 3474
		const val GL_MAP1_TEXTURE_COORD_1 = 3475
		const val GL_MAP1_TEXTURE_COORD_2 = 3476
		const val GL_MAP1_TEXTURE_COORD_3 = 3477
		const val GL_MAP1_TEXTURE_COORD_4 = 3478
		const val GL_MAP1_VERTEX_3 = 3479
		const val GL_MAP1_VERTEX_4 = 3480
		const val GL_MAP2_COLOR_4 = 3504
		const val GL_MAP2_INDEX = 3505
		const val GL_MAP2_NORMAL = 3506
		const val GL_MAP2_TEXTURE_COORD_1 = 3507
		const val GL_MAP2_TEXTURE_COORD_2 = 3508
		const val GL_MAP2_TEXTURE_COORD_3 = 3509
		const val GL_MAP2_TEXTURE_COORD_4 = 3510
		const val GL_MAP2_VERTEX_3 = 3511
		const val GL_MAP2_VERTEX_4 = 3512
		const val GL_MAP1_GRID_DOMAIN = 3536
		const val GL_MAP1_GRID_SEGMENTS = 3537
		const val GL_MAP2_GRID_DOMAIN = 3538
		const val GL_MAP2_GRID_SEGMENTS = 3539
		const val GL_TEXTURE_1D = 3552
		const val GL_TEXTURE_2D = 3553
		const val GL_FEEDBACK_BUFFER_POINTER = 3568
		const val GL_FEEDBACK_BUFFER_SIZE = 3569
		const val GL_FEEDBACK_BUFFER_TYPE = 3570
		const val GL_SELECTION_BUFFER_POINTER = 3571
		const val GL_SELECTION_BUFFER_SIZE = 3572


		/** GetTextureParameter */
		const val GL_TEXTURE_WIDTH = 4096
		const val GL_TEXTURE_HEIGHT = 4097
		const val GL_TEXTURE_INTERNAL_FORMAT = 4099
		const val GL_TEXTURE_COMPONENTS = 4099
		const val GL_TEXTURE_BORDER_COLOR = 4100
		const val GL_TEXTURE_BORDER = 4101


		/** HintMode */
		const val GL_DONT_CARE = 4352
		const val GL_FASTEST = 4353
		const val GL_NICEST = 4354


		/** LightName */
		const val GL_LIGHT0 = 16384
		const val GL_LIGHT1 = 16385
		const val GL_LIGHT2 = 16386
		const val GL_LIGHT3 = 16387
		const val GL_LIGHT4 = 16388
		const val GL_LIGHT5 = 16389
		const val GL_LIGHT6 = 16390
		const val GL_LIGHT7 = 16391


		/** LightParameter */
		const val GL_AMBIENT = 4608
		const val GL_DIFFUSE = 4609
		const val GL_SPECULAR = 4610
		const val GL_POSITION = 4611
		const val GL_SPOT_DIRECTION = 4612
		const val GL_SPOT_EXPONENT = 4613
		const val GL_SPOT_CUTOFF = 4614
		const val GL_CONSTANT_ATTENUATION = 4615
		const val GL_LINEAR_ATTENUATION = 4616
		const val GL_QUADRATIC_ATTENUATION = 4617


		/** ListMode */
		const val GL_COMPILE = 4864
		const val GL_COMPILE_AND_EXECUTE = 4865


		/** LogicOp */
		const val GL_CLEAR = 5376
		const val GL_AND = 5377
		const val GL_AND_REVERSE = 5378
		const val GL_COPY = 5379
		const val GL_AND_INVERTED = 5380
		const val GL_NOOP = 5381
		const val GL_XOR = 5382
		const val GL_OR = 5383
		const val GL_NOR = 5384
		const val GL_EQUIV = 5385
		const val GL_INVERT = 5386
		const val GL_OR_REVERSE = 5387
		const val GL_COPY_INVERTED = 5388
		const val GL_OR_INVERTED = 5389
		const val GL_NAND = 5390
		const val GL_SET = 5391


		/** MaterialParameter */
		const val GL_EMISSION = 5632
		const val GL_SHININESS = 5633
		const val GL_AMBIENT_AND_DIFFUSE = 5634
		const val GL_COLOR_INDEXES = 5635


		/** MatrixMode */
		const val GL_MODELVIEW = 5888
		const val GL_PROJECTION = 5889
		const val GL_TEXTURE = 5890


		/** PixelCopyType */
		const val GL_COLOR = 6144
		const val GL_DEPTH = 6145
		const val GL_STENCIL = 6146


		/** PixelFormat */
		const val GL_COLOR_INDEX = 6400
		const val GL_STENCIL_INDEX = 6401
		const val GL_DEPTH_COMPONENT = 6402
		const val GL_RED = 6403
		const val GL_GREEN = 6404
		const val GL_BLUE = 6405
		const val GL_ALPHA = 6406
		const val GL_RGB = 6407
		const val GL_RGBA = 6408
		const val GL_LUMINANCE = 6409
		const val GL_LUMINANCE_ALPHA = 6410


		/** PixelType */
		const val GL_BITMAP = 6656


		/** PolygonMode */
		const val GL_POINT = 6912
		const val GL_LINE = 6913
		const val GL_FILL = 6914


		/** RenderingMode */
		const val GL_RENDER = 7168
		const val GL_FEEDBACK = 7169
		const val GL_SELECT = 7170


		/** ShadingModel */
		const val GL_FLAT = 7424
		const val GL_SMOOTH = 7425


		/** StencilOp */
		const val GL_KEEP = 7680
		const val GL_REPLACE = 7681
		const val GL_INCR = 7682
		const val GL_DECR = 7683


		/** StringName */
		const val GL_VENDOR = 7936
		const val GL_RENDERER = 7937
		const val GL_VERSION = 7938
		const val GL_EXTENSIONS = 7939


		/** TextureCoordName */
		const val GL_S = 8192
		const val GL_T = 8193
		const val GL_R = 8194
		const val GL_Q = 8195


		/** TextureEnvMode */
		const val GL_MODULATE = 8448
		const val GL_DECAL = 8449


		/** TextureEnvParameter */
		const val GL_TEXTURE_ENV_MODE = 8704
		const val GL_TEXTURE_ENV_COLOR = 8705


		/** TextureEnvTarget */
		const val GL_TEXTURE_ENV = 8960


		/** TextureGenMode */
		const val GL_EYE_LINEAR = 9216
		const val GL_OBJECT_LINEAR = 9217
		const val GL_SPHERE_MAP = 9218


		/** TextureGenParameter */
		const val GL_TEXTURE_GEN_MODE = 9472
		const val GL_OBJECT_PLANE = 9473
		const val GL_EYE_PLANE = 9474


		/** TextureMagFilter */
		const val GL_NEAREST = 9728
		const val GL_LINEAR = 9729


		/** TextureMinFilter */
		const val GL_NEAREST_MIPMAP_NEAREST = 9984
		const val GL_LINEAR_MIPMAP_NEAREST = 9985
		const val GL_NEAREST_MIPMAP_LINEAR = 9986
		const val GL_LINEAR_MIPMAP_LINEAR = 9987


		/** TextureParameterName */
		const val GL_TEXTURE_MAG_FILTER = 10240
		const val GL_TEXTURE_MIN_FILTER = 10241
		const val GL_TEXTURE_WRAP_S = 10242
		const val GL_TEXTURE_WRAP_T = 10243


		/** TextureWrapMode */
		const val GL_CLAMP = 10496
		const val GL_REPEAT = 10497


		/** ClientAttribMask */
		const val GL_CLIENT_PIXEL_STORE_BIT = 1
		const val GL_CLIENT_VERTEX_ARRAY_BIT = 2
		const val GL_CLIENT_ALL_ATTRIB_BITS = -1


		/** polygon_offset */
		const val GL_POLYGON_OFFSET_FACTOR = 32824
		const val GL_POLYGON_OFFSET_UNITS = 10752
		const val GL_POLYGON_OFFSET_POINT = 10753
		const val GL_POLYGON_OFFSET_LINE = 10754
		const val GL_POLYGON_OFFSET_FILL = 32823


		/** texture */
		const val GL_ALPHA4 = 32827
		const val GL_ALPHA8 = 32828
		const val GL_ALPHA12 = 32829
		const val GL_ALPHA16 = 32830
		const val GL_LUMINANCE4 = 32831
		const val GL_LUMINANCE8 = 32832
		const val GL_LUMINANCE12 = 32833
		const val GL_LUMINANCE16 = 32834
		const val GL_LUMINANCE4_ALPHA4 = 32835
		const val GL_LUMINANCE6_ALPHA2 = 32836
		const val GL_LUMINANCE8_ALPHA8 = 32837
		const val GL_LUMINANCE12_ALPHA4 = 32838
		const val GL_LUMINANCE12_ALPHA12 = 32839
		const val GL_LUMINANCE16_ALPHA16 = 32840
		const val GL_INTENSITY = 32841
		const val GL_INTENSITY4 = 32842
		const val GL_INTENSITY8 = 32843
		const val GL_INTENSITY12 = 32844
		const val GL_INTENSITY16 = 32845
		const val GL_R3_G3_B2 = 10768
		const val GL_RGB4 = 32847
		const val GL_RGB5 = 32848
		const val GL_RGB8 = 32849
		const val GL_RGB10 = 32850
		const val GL_RGB12 = 32851
		const val GL_RGB16 = 32852
		const val GL_RGBA2 = 32853
		const val GL_RGBA4 = 32854
		const val GL_RGB5_A1 = 32855
		const val GL_RGBA8 = 32856
		const val GL_RGB10_A2 = 32857
		const val GL_RGBA12 = 32858
		const val GL_RGBA16 = 32859
		const val GL_TEXTURE_RED_SIZE = 32860
		const val GL_TEXTURE_GREEN_SIZE = 32861
		const val GL_TEXTURE_BLUE_SIZE = 32862
		const val GL_TEXTURE_ALPHA_SIZE = 32863
		const val GL_TEXTURE_LUMINANCE_SIZE = 32864
		const val GL_TEXTURE_INTENSITY_SIZE = 32865
		const val GL_PROXY_TEXTURE_1D = 32867
		const val GL_PROXY_TEXTURE_2D = 32868


		/** texture_object */
		const val GL_TEXTURE_PRIORITY = 32870
		const val GL_TEXTURE_RESIDENT = 32871
		const val GL_TEXTURE_BINDING_1D = 32872
		const val GL_TEXTURE_BINDING_2D = 32873


		/** vertex_array */
		const val GL_VERTEX_ARRAY = 32884
		const val GL_NORMAL_ARRAY = 32885
		const val GL_COLOR_ARRAY = 32886
		const val GL_INDEX_ARRAY = 32887
		const val GL_TEXTURE_COORD_ARRAY = 32888
		const val GL_EDGE_FLAG_ARRAY = 32889
		const val GL_VERTEX_ARRAY_SIZE = 32890
		const val GL_VERTEX_ARRAY_TYPE = 32891
		const val GL_VERTEX_ARRAY_STRIDE = 32892
		const val GL_NORMAL_ARRAY_TYPE = 32894
		const val GL_NORMAL_ARRAY_STRIDE = 32895
		const val GL_COLOR_ARRAY_SIZE = 32897
		const val GL_COLOR_ARRAY_TYPE = 32898
		const val GL_COLOR_ARRAY_STRIDE = 32899
		const val GL_INDEX_ARRAY_TYPE = 32901
		const val GL_INDEX_ARRAY_STRIDE = 32902
		const val GL_TEXTURE_COORD_ARRAY_SIZE = 32904
		const val GL_TEXTURE_COORD_ARRAY_TYPE = 32905
		const val GL_TEXTURE_COORD_ARRAY_STRIDE = 32906
		const val GL_EDGE_FLAG_ARRAY_STRIDE = 32908
		const val GL_VERTEX_ARRAY_POINTER = 32910
		const val GL_NORMAL_ARRAY_POINTER = 32911
		const val GL_COLOR_ARRAY_POINTER = 32912
		const val GL_INDEX_ARRAY_POINTER = 32913
		const val GL_TEXTURE_COORD_ARRAY_POINTER = 32914
		const val GL_EDGE_FLAG_ARRAY_POINTER = 32915
		const val GL_V2F = 10784
		const val GL_V3F = 10785
		const val GL_C4UB_V2F = 10786
		const val GL_C4UB_V3F = 10787
		const val GL_C3F_V3F = 10788
		const val GL_N3F_V3F = 10789
		const val GL_C4F_N3F_V3F = 10790
		const val GL_T2F_V3F = 10791
		const val GL_T4F_V4F = 10792
		const val GL_T2F_C4UB_V3F = 10793
		const val GL_T2F_C3F_V3F = 10794
		const val GL_T2F_N3F_V3F = 10795
		const val GL_T2F_C4F_N3F_V3F = 10796
		const val GL_T4F_C4F_N3F_V4F = 10797


		/** Aliases for smooth points and lines. */
		const val GL_ALIASED_POINT_SIZE_RANGE = 33901
		const val GL_ALIASED_LINE_WIDTH_RANGE = 33902
		const val GL_SMOOTH_POINT_SIZE_RANGE = 2834
		const val GL_SMOOTH_POINT_SIZE_GRANULARITY = 2835
		const val GL_SMOOTH_LINE_WIDTH_RANGE = 2850
		const val GL_SMOOTH_LINE_WIDTH_GRANULARITY = 2851


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_TEXTURE_BINDING_3D = 32874


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev, and by the {@code pname} parameter of PixelStore. */
		const val GL_PACK_SKIP_IMAGES = 32875
		const val GL_PACK_IMAGE_HEIGHT = 32876
		const val GL_UNPACK_SKIP_IMAGES = 32877
		const val GL_UNPACK_IMAGE_HEIGHT = 32878


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev, and by the {@code target} parameter of TexImage3D, GetTexImage, GetTexLevelParameteriv, GetTexLevelParameterfv, GetTexParameteriv, and
		* GetTexParameterfv.
		*/
		const val GL_TEXTURE_3D = 32879


		/** Accepted by the {@code target} parameter of TexImage3D, GetTexLevelParameteriv, and GetTexLevelParameterfv. */
		const val GL_PROXY_TEXTURE_3D = 32880


		/** Accepted by the {@code pname} parameter of GetTexLevelParameteriv and GetTexLevelParameterfv. */
		const val GL_TEXTURE_DEPTH = 32881


		/** Accepted by the {@code pname} parameter of TexParameteriv, TexParameterfv, GetTexParameteriv, and GetTexParameterfv. */
		const val GL_TEXTURE_WRAP_R = 32882


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_3D_TEXTURE_SIZE = 32883


		/** Accepted by the {@code format} parameter of DrawPixels, GetTexImage, ReadPixels, TexImage1D, and TexImage2D. */
		const val GL_BGR = 32992
		const val GL_BGRA = 32993


		/**
		* Accepted by the {@code type} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, GetTexImage, TexImage3D, TexSubImage1D, TexSubImage2D,
		* TexSubImage3D, GetHistogram, GetMinmax, ConvolutionFilter1D, ConvolutionFilter2D, ConvolutionFilter3D, GetConvolutionFilter, SeparableFilter2D,
		* SeparableFilter3D, GetSeparableFilter, ColorTable, GetColorTable, TexImage4D, and TexSubImage4D.
		*/
		const val GL_UNSIGNED_BYTE_3_3_2 = 32818
		const val GL_UNSIGNED_BYTE_2_3_3_REV = 33634
		const val GL_UNSIGNED_SHORT_5_6_5 = 33635
		const val GL_UNSIGNED_SHORT_5_6_5_REV = 33636
		const val GL_UNSIGNED_SHORT_4_4_4_4 = 32819
		const val GL_UNSIGNED_SHORT_4_4_4_4_REV = 33637
		const val GL_UNSIGNED_SHORT_5_5_5_1 = 32820
		const val GL_UNSIGNED_SHORT_1_5_5_5_REV = 33638
		const val GL_UNSIGNED_INT_8_8_8_8 = 32821
		const val GL_UNSIGNED_INT_8_8_8_8_REV = 33639
		const val GL_UNSIGNED_INT_10_10_10_2 = 32822
		const val GL_UNSIGNED_INT_2_10_10_10_REV = 33640


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_RESCALE_NORMAL = 32826


		/** Accepted by the {@code pname} parameter of LightModel*, and also by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_LIGHT_MODEL_COLOR_CONTROL = 33272


		/** Accepted by the {@code param} parameter of LightModel* when {@code pname} is  LIGHT_MODEL_COLOR_CONTROL. */
		const val GL_SINGLE_COLOR = 33273
		const val GL_SEPARATE_SPECULAR_COLOR = 33274


		/**
		* Accepted by the {@code param} parameter of TexParameteri and TexParameterf, and by the {@code params} parameter of TexParameteriv and TexParameterfv,
		* when their {@code pname} parameter is TEXTURE_WRAP_S, TEXTURE_WRAP_T, or TEXTURE_WRAP_R.
		*/
		const val GL_CLAMP_TO_EDGE = 33071


		/** Accepted by the {@code pname} parameter of TexParameteri, TexParameterf, TexParameteriv, TexParameterfv, GetTexParameteriv, and GetTexParameterfv. */
		const val GL_TEXTURE_MIN_LOD = 33082
		const val GL_TEXTURE_MAX_LOD = 33083
		const val GL_TEXTURE_BASE_LEVEL = 33084
		const val GL_TEXTURE_MAX_LEVEL = 33085


		/** Recommended maximum amounts of vertex and index data. */
		const val GL_MAX_ELEMENTS_VERTICES = 33000
		const val GL_MAX_ELEMENTS_INDICES = 33001


		/** Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, and CopyTexImage2D. */
		const val GL_COMPRESSED_ALPHA = 34025
		const val GL_COMPRESSED_LUMINANCE = 34026
		const val GL_COMPRESSED_LUMINANCE_ALPHA = 34027
		const val GL_COMPRESSED_INTENSITY = 34028
		const val GL_COMPRESSED_RGB = 34029
		const val GL_COMPRESSED_RGBA = 34030


		/** Accepted by the {@code target} parameter of Hint and the {@code value} parameter of GetIntegerv, GetBooleanv, GetFloatv, and GetDoublev. */
		const val GL_TEXTURE_COMPRESSION_HINT = 34031


		/** Accepted by the {@code value} parameter of GetTexLevelParameter. */
		const val GL_TEXTURE_COMPRESSED_IMAGE_SIZE = 34464
		const val GL_TEXTURE_COMPRESSED = 34465


		/** Accepted by the {@code value} parameter of GetIntegerv, GetBooleanv, GetFloatv, and GetDoublev. */
		const val GL_NUM_COMPRESSED_TEXTURE_FORMATS = 34466
		const val GL_COMPRESSED_TEXTURE_FORMATS = 34467


		/** Accepted by the {@code param} parameters of TexGend, TexGenf, and TexGeni when {@code pname} parameter is TEXTURE_GEN_MODE. */
		const val GL_NORMAL_MAP = 34065
		const val GL_REFLECTION_MAP = 34066


		/**
		* When the {@code pname} parameter of TexGendv, TexGenfv, and TexGeniv is TEXTURE_GEN_MODE, then the array {@code params} may also contain NORMAL_MAP
		* or REFLECTION_MAP. Accepted by the {@code cap} parameter of Enable, Disable, IsEnabled, and by the {@code pname} parameter of GetBooleanv,
		* GetIntegerv, GetFloatv, and GetDoublev, and by the {@code target} parameter of BindTexture, GetTexParameterfv, GetTexParameteriv, TexParameterf,
		* TexParameteri, TexParameterfv, and TexParameteriv.
		*/
		const val GL_TEXTURE_CUBE_MAP = 34067


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_TEXTURE_BINDING_CUBE_MAP = 34068


		/**
		* Accepted by the {@code target} parameter of GetTexImage, GetTexLevelParameteriv, GetTexLevelParameterfv, TexImage2D, CopyTexImage2D, TexSubImage2D, and
		* CopySubTexImage2D.
		*/
		const val GL_TEXTURE_CUBE_MAP_POSITIVE_X = 34069
		const val GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 34070
		const val GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 34071
		const val GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 34072
		const val GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 34073
		const val GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 34074


		/** Accepted by the {@code target} parameter of GetTexLevelParameteriv, GetTexLevelParameterfv, GetTexParameteriv, and TexImage2D. */
		const val GL_PROXY_TEXTURE_CUBE_MAP = 34075


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_MAX_CUBE_MAP_TEXTURE_SIZE = 34076


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_MULTISAMPLE = 32925
		const val GL_SAMPLE_ALPHA_TO_COVERAGE = 32926
		const val GL_SAMPLE_ALPHA_TO_ONE = 32927
		const val GL_SAMPLE_COVERAGE = 32928


		/** Accepted by the {@code details} parameter of PushAttrib. */
		const val GL_MULTISAMPLE_BIT = 536870912


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_SAMPLE_BUFFERS = 32936
		const val GL_SAMPLES = 32937
		const val GL_SAMPLE_COVERAGE_VALUE = 32938
		const val GL_SAMPLE_COVERAGE_INVERT = 32939


		/** Accepted by the {@code texture} parameter of ActiveTexture and MultiTexCoord. */
		const val GL_TEXTURE0 = 33984
		const val GL_TEXTURE1 = 33985
		const val GL_TEXTURE2 = 33986
		const val GL_TEXTURE3 = 33987
		const val GL_TEXTURE4 = 33988
		const val GL_TEXTURE5 = 33989
		const val GL_TEXTURE6 = 33990
		const val GL_TEXTURE7 = 33991
		const val GL_TEXTURE8 = 33992
		const val GL_TEXTURE9 = 33993
		const val GL_TEXTURE10 = 33994
		const val GL_TEXTURE11 = 33995
		const val GL_TEXTURE12 = 33996
		const val GL_TEXTURE13 = 33997
		const val GL_TEXTURE14 = 33998
		const val GL_TEXTURE15 = 33999
		const val GL_TEXTURE16 = 34000
		const val GL_TEXTURE17 = 34001
		const val GL_TEXTURE18 = 34002
		const val GL_TEXTURE19 = 34003
		const val GL_TEXTURE20 = 34004
		const val GL_TEXTURE21 = 34005
		const val GL_TEXTURE22 = 34006
		const val GL_TEXTURE23 = 34007
		const val GL_TEXTURE24 = 34008
		const val GL_TEXTURE25 = 34009
		const val GL_TEXTURE26 = 34010
		const val GL_TEXTURE27 = 34011
		const val GL_TEXTURE28 = 34012
		const val GL_TEXTURE29 = 34013
		const val GL_TEXTURE30 = 34014
		const val GL_TEXTURE31 = 34015


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_ACTIVE_TEXTURE = 34016
		const val GL_CLIENT_ACTIVE_TEXTURE = 34017
		const val GL_MAX_TEXTURE_UNITS = 34018


		/** Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is TEXTURE_ENV_MODE. */
		const val GL_COMBINE = 34160


		/** Accepted by the {@code pname} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code target} parameter value is TEXTURE_ENV. */
		const val GL_COMBINE_RGB = 34161
		const val GL_COMBINE_ALPHA = 34162
		const val GL_SOURCE0_RGB = 34176
		const val GL_SOURCE1_RGB = 34177
		const val GL_SOURCE2_RGB = 34178
		const val GL_SOURCE0_ALPHA = 34184
		const val GL_SOURCE1_ALPHA = 34185
		const val GL_SOURCE2_ALPHA = 34186
		const val GL_OPERAND0_RGB = 34192
		const val GL_OPERAND1_RGB = 34193
		const val GL_OPERAND2_RGB = 34194
		const val GL_OPERAND0_ALPHA = 34200
		const val GL_OPERAND1_ALPHA = 34201
		const val GL_OPERAND2_ALPHA = 34202
		const val GL_RGB_SCALE = 34163


		/**
		* Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is COMBINE_RGB or
		* COMBINE_ALPHA.
		*/
		const val GL_ADD_SIGNED = 34164
		const val GL_INTERPOLATE = 34165
		const val GL_SUBTRACT = 34023


		/**
		* Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is SOURCE0_RGB,
		* SOURCE1_RGB, SOURCE2_RGB, SOURCE0_ALPHA, SOURCE1_ALPHA, or SOURCE2_ALPHA.
		*/
		const val GL_CONSTANT = 34166
		const val GL_PRIMARY_COLOR = 34167
		const val GL_PREVIOUS = 34168


		/** Accepted by the {@code params} parameter of TexEnvf, TexEnvi, TexEnvfv, and TexEnviv when the {@code pname} parameter value is COMBINE_RGB_ARB. */
		const val GL_DOT3_RGB = 34478
		const val GL_DOT3_RGBA = 34479


		/**
		* Accepted by the {@code param} parameter of TexParameteri and TexParameterf, and by the {@code params} parameter of TexParameteriv and TexParameterfv,
		* when their {@code pname} parameter is TEXTURE_WRAP_S, TEXTURE_WRAP_T, or TEXTURE_WRAP_R.
		*/
		const val GL_CLAMP_TO_BORDER = 33069


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_TRANSPOSE_MODELVIEW_MATRIX = 34019
		const val GL_TRANSPOSE_PROJECTION_MATRIX = 34020
		const val GL_TRANSPOSE_TEXTURE_MATRIX = 34021
		const val GL_TRANSPOSE_COLOR_MATRIX = 34022


		/** Accepted by the {@code pname} parameter of TexParameteri, TexParameterf, TexParameteriv, TexParameterfv, GetTexParameteriv, and GetTexParameterfv. */
		const val GL_GENERATE_MIPMAP = 33169


		/** Accepted by the {@code target} parameter of Hint, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_GENERATE_MIPMAP_HINT = 33170


		/** Accepted by the {@code sfactor} and {@code dfactor} parameters of BlendFunc. */
		const val GL_CONSTANT_COLOR = 32769
		const val GL_ONE_MINUS_CONSTANT_COLOR = 32770
		const val GL_CONSTANT_ALPHA = 32771
		const val GL_ONE_MINUS_CONSTANT_ALPHA = 32772


		/** Accepted by the {@code mode} parameter of BlendEquation. */
		const val GL_FUNC_ADD = 32774
		const val GL_MIN = 32775
		const val GL_MAX = 32776


		/** Accepted by the {@code mode} parameter of BlendEquation. */
		const val GL_FUNC_SUBTRACT = 32778
		const val GL_FUNC_REVERSE_SUBTRACT = 32779


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, CopyTexImage1D and CopyTexImage2D. */
		const val GL_DEPTH_COMPONENT16 = 33189
		const val GL_DEPTH_COMPONENT24 = 33190
		const val GL_DEPTH_COMPONENT32 = 33191


		/** Accepted by the {@code pname} parameter of GetTexLevelParameterfv and GetTexLevelParameteriv. */
		const val GL_TEXTURE_DEPTH_SIZE = 34890


		/** Accepted by the {@code pname} parameter of TexParameterf, TexParameteri, TexParameterfv, TexParameteriv, GetTexParameterfv, and GetTexParameteriv. */
		const val GL_DEPTH_TEXTURE_MODE = 34891


		/** Accepted by the {@code pname} parameter of TexParameterf, TexParameteri, TexParameterfv, TexParameteriv, GetTexParameterfv, and GetTexParameteriv. */
		const val GL_TEXTURE_COMPARE_MODE = 34892
		const val GL_TEXTURE_COMPARE_FUNC = 34893


		/**
		* Accepted by the {@code param} parameter of TexParameterf, TexParameteri, TexParameterfv, and TexParameteriv when the {@code pname} parameter is
		* TEXTURE_COMPARE_MODE.
		*/
		const val GL_COMPARE_R_TO_TEXTURE = 34894


		/** Accepted by the {@code pname} parameter of Fogi and Fogf. */
		const val GL_FOG_COORDINATE_SOURCE = 33872


		/** Accepted by the {@code param} parameter of Fogi and Fogf. */
		const val GL_FOG_COORDINATE = 33873
		const val GL_FRAGMENT_DEPTH = 33874


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_CURRENT_FOG_COORDINATE = 33875
		const val GL_FOG_COORDINATE_ARRAY_TYPE = 33876
		const val GL_FOG_COORDINATE_ARRAY_STRIDE = 33877


		/** Accepted by the {@code pname} parameter of GetPointerv. */
		const val GL_FOG_COORDINATE_ARRAY_POINTER = 33878


		/** Accepted by the {@code array} parameter of EnableClientState and DisableClientState. */
		const val GL_FOG_COORDINATE_ARRAY = 33879


		/** Accepted by the {@code pname} parameter of PointParameterfARB, and the {@code pname} of Get. */
		const val GL_POINT_SIZE_MIN = 33062
		const val GL_POINT_SIZE_MAX = 33063
		const val GL_POINT_FADE_THRESHOLD_SIZE = 33064
		const val GL_POINT_DISTANCE_ATTENUATION = 33065


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_COLOR_SUM = 33880


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_CURRENT_SECONDARY_COLOR = 33881
		const val GL_SECONDARY_COLOR_ARRAY_SIZE = 33882
		const val GL_SECONDARY_COLOR_ARRAY_TYPE = 33883
		const val GL_SECONDARY_COLOR_ARRAY_STRIDE = 33884


		/** Accepted by the {@code pname} parameter of GetPointerv. */
		const val GL_SECONDARY_COLOR_ARRAY_POINTER = 33885


		/** Accepted by the {@code array} parameter of EnableClientState and DisableClientState. */
		const val GL_SECONDARY_COLOR_ARRAY = 33886


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_BLEND_DST_RGB = 32968
		const val GL_BLEND_SRC_RGB = 32969
		const val GL_BLEND_DST_ALPHA = 32970
		const val GL_BLEND_SRC_ALPHA = 32971


		/** Accepted by the {@code sfail}, {@code dpfail}, and {@code dppass} parameter of StencilOp. */
		const val GL_INCR_WRAP = 34055
		const val GL_DECR_WRAP = 34056


		/** Accepted by the {@code target} parameters of GetTexEnvfv, GetTexEnviv, TexEnvi, TexEnvf, Texenviv, and TexEnvfv. */
		const val GL_TEXTURE_FILTER_CONTROL = 34048


		/**
		* When the {@code target} parameter of GetTexEnvfv, GetTexEnviv, TexEnvi, TexEnvf, TexEnviv, and TexEnvfv is TEXTURE_FILTER_CONTROL, then the value of
		* {@code pname} may be.
		*/
		const val GL_TEXTURE_LOD_BIAS = 34049


		/** Accepted by the {@code pname} parameters of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_TEXTURE_LOD_BIAS = 34045


		/**
		* Accepted by the {@code param} parameter of TexParameteri and TexParameterf, and by the {@code params} parameter of TexParameteriv and TexParameterfv,
		* when their {@code pname} parameter is TEXTURE_WRAP_S, TEXTURE_WRAP_T, or TEXTURE_WRAP_R.
		*/
		const val GL_MIRRORED_REPEAT = 33648

		/** New token names. */
		const val GL_FOG_COORD_SRC = 33872
		const val GL_FOG_COORD = 33873
		const val GL_CURRENT_FOG_COORD = 33875
		const val GL_FOG_COORD_ARRAY_TYPE = 33876
		const val GL_FOG_COORD_ARRAY_STRIDE = 33877
		const val GL_FOG_COORD_ARRAY_POINTER = 33878
		const val GL_FOG_COORD_ARRAY = 33879
		const val GL_FOG_COORD_ARRAY_BUFFER_BINDING = 34973
		const val GL_SRC0_RGB = 34176
		const val GL_SRC1_RGB = 34177
		const val GL_SRC2_RGB = 34178
		const val GL_SRC0_ALPHA = 34184
		const val GL_SRC1_ALPHA = 34185
		const val GL_SRC2_ALPHA = 34186


		/**
		* Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData,
		* GetBufferParameteriv, and GetBufferPointerv.
		*/
		const val GL_ARRAY_BUFFER = 34962
		const val GL_ELEMENT_ARRAY_BUFFER = 34963


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_ARRAY_BUFFER_BINDING = 34964
		const val GL_ELEMENT_ARRAY_BUFFER_BINDING = 34965
		const val GL_VERTEX_ARRAY_BUFFER_BINDING = 34966
		const val GL_NORMAL_ARRAY_BUFFER_BINDING = 34967
		const val GL_COLOR_ARRAY_BUFFER_BINDING = 34968
		const val GL_INDEX_ARRAY_BUFFER_BINDING = 34969
		const val GL_TEXTURE_COORD_ARRAY_BUFFER_BINDING = 34970
		const val GL_EDGE_FLAG_ARRAY_BUFFER_BINDING = 34971
		const val GL_SECONDARY_COLOR_ARRAY_BUFFER_BINDING = 34972
		const val GL_FOG_COORDINATE_ARRAY_BUFFER_BINDING = 34973
		const val GL_WEIGHT_ARRAY_BUFFER_BINDING = 34974


		/** Accepted by the {@code pname} parameter of GetVertexAttribiv. */
		const val GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING = 34975


		/** Accepted by the {@code usage} parameter of BufferData. */
		const val GL_STREAM_DRAW = 35040
		const val GL_STREAM_READ = 35041
		const val GL_STREAM_COPY = 35042
		const val GL_STATIC_DRAW = 35044
		const val GL_STATIC_READ = 35045
		const val GL_STATIC_COPY = 35046
		const val GL_DYNAMIC_DRAW = 35048
		const val GL_DYNAMIC_READ = 35049
		const val GL_DYNAMIC_COPY = 35050


		/** Accepted by the {@code access} parameter of MapBuffer. */
		const val GL_READ_ONLY = 35000
		const val GL_WRITE_ONLY = 35001
		const val GL_READ_WRITE = 35002


		/** Accepted by the {@code pname} parameter of GetBufferParameteriv. */
		const val GL_BUFFER_SIZE = 34660
		const val GL_BUFFER_USAGE = 34661
		const val GL_BUFFER_ACCESS = 35003
		const val GL_BUFFER_MAPPED = 35004


		/** Accepted by the {@code pname} parameter of GetBufferPointerv. */
		const val GL_BUFFER_MAP_POINTER = 35005


		/** Accepted by the {@code target} parameter of BeginQuery, EndQuery, and GetQueryiv. */
		const val GL_SAMPLES_PASSED = 35092


		/** Accepted by the {@code pname} parameter of GetQueryiv. */
		const val GL_QUERY_COUNTER_BITS = 34916
		const val GL_CURRENT_QUERY = 34917


		/** Accepted by the {@code pname} parameter of GetQueryObjectiv and GetQueryObjectuiv. */
		const val GL_QUERY_RESULT = 34918
		const val GL_QUERY_RESULT_AVAILABLE = 34919


		/** Accepted by the {@code name} parameter of GetString. */
		const val GL_SHADING_LANGUAGE_VERSION = 35724


		/** Accepted by the {@code pname} parameter of GetInteger. */
		const val GL_CURRENT_PROGRAM = 35725


		/** Accepted by the {@code pname} parameter of GetShaderiv. */
		const val GL_SHADER_TYPE = 35663
		const val GL_DELETE_STATUS = 35712
		const val GL_COMPILE_STATUS = 35713
		const val GL_LINK_STATUS = 35714
		const val GL_VALIDATE_STATUS = 35715
		const val GL_INFO_LOG_LENGTH = 35716
		const val GL_ATTACHED_SHADERS = 35717
		const val GL_ACTIVE_UNIFORMS = 35718
		const val GL_ACTIVE_UNIFORM_MAX_LENGTH = 35719
		const val GL_ACTIVE_ATTRIBUTES = 35721
		const val GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 35722
		const val GL_SHADER_SOURCE_LENGTH = 35720


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GL_FLOAT_VEC2 = 35664
		const val GL_FLOAT_VEC3 = 35665
		const val GL_FLOAT_VEC4 = 35666
		const val GL_INT_VEC2 = 35667
		const val GL_INT_VEC3 = 35668
		const val GL_INT_VEC4 = 35669
		const val GL_BOOL = 35670
		const val GL_BOOL_VEC2 = 35671
		const val GL_BOOL_VEC3 = 35672
		const val GL_BOOL_VEC4 = 35673
		const val GL_FLOAT_MAT2 = 35674
		const val GL_FLOAT_MAT3 = 35675
		const val GL_FLOAT_MAT4 = 35676
		const val GL_SAMPLER_1D = 35677
		const val GL_SAMPLER_2D = 35678
		const val GL_SAMPLER_3D = 35679
		const val GL_SAMPLER_CUBE = 35680
		const val GL_SAMPLER_1D_SHADOW = 35681
		const val GL_SAMPLER_2D_SHADOW = 35682


		/** Accepted by the {@code type} argument of CreateShader and returned by the {@code params} parameter of GetShaderiv. */
		const val GL_VERTEX_SHADER = 35633


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_VERTEX_UNIFORM_COMPONENTS = 35658
		const val GL_MAX_VARYING_FLOATS = 35659
		const val GL_MAX_VERTEX_ATTRIBS = 34921
		const val GL_MAX_TEXTURE_IMAGE_UNITS = 34930
		const val GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 35660
		const val GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 35661
		const val GL_MAX_TEXTURE_COORDS = 34929


		/**
		* Accepted by the {@code cap} parameter of Disable, Enable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_VERTEX_PROGRAM_POINT_SIZE = 34370
		const val GL_VERTEX_PROGRAM_TWO_SIDE = 34371


		/** Accepted by the {@code pname} parameter of GetVertexAttrib{dfi}v. */
		const val GL_VERTEX_ATTRIB_ARRAY_ENABLED = 34338
		const val GL_VERTEX_ATTRIB_ARRAY_SIZE = 34339
		const val GL_VERTEX_ATTRIB_ARRAY_STRIDE = 34340
		const val GL_VERTEX_ATTRIB_ARRAY_TYPE = 34341
		const val GL_VERTEX_ATTRIB_ARRAY_NORMALIZED = 34922
		const val GL_CURRENT_VERTEX_ATTRIB = 34342


		/** Accepted by the {@code pname} parameter of GetVertexAttribPointerv. */
		const val GL_VERTEX_ATTRIB_ARRAY_POINTER = 34373


		/** Accepted by the {@code type} argument of CreateShader and returned by the {@code params} parameter of GetShaderiv. */
		const val GL_FRAGMENT_SHADER = 35632


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 35657


		/** Accepted by the {@code target} parameter of Hint and the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_FRAGMENT_SHADER_DERIVATIVE_HINT = 35723


		/** Accepted by the {@code pname} parameters of GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_DRAW_BUFFERS = 34852
		const val GL_DRAW_BUFFER0 = 34853
		const val GL_DRAW_BUFFER1 = 34854
		const val GL_DRAW_BUFFER2 = 34855
		const val GL_DRAW_BUFFER3 = 34856
		const val GL_DRAW_BUFFER4 = 34857
		const val GL_DRAW_BUFFER5 = 34858
		const val GL_DRAW_BUFFER6 = 34859
		const val GL_DRAW_BUFFER7 = 34860
		const val GL_DRAW_BUFFER8 = 34861
		const val GL_DRAW_BUFFER9 = 34862
		const val GL_DRAW_BUFFER10 = 34863
		const val GL_DRAW_BUFFER11 = 34864
		const val GL_DRAW_BUFFER12 = 34865
		const val GL_DRAW_BUFFER13 = 34866
		const val GL_DRAW_BUFFER14 = 34867
		const val GL_DRAW_BUFFER15 = 34868


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev, and by the {@code target} parameter of TexEnvi, TexEnviv, TexEnvf, TexEnvfv, GetTexEnviv, and GetTexEnvfv.
		*/
		const val GL_POINT_SPRITE = 34913


		/**
		* When the {@code target} parameter of TexEnvf, TexEnvfv, TexEnvi, TexEnviv, GetTexEnvfv, or GetTexEnviv is POINT_SPRITE, then the value of
		* {@code pname} may be.
		*/
		const val GL_COORD_REPLACE = 34914


		/** Accepted by the {@code pname} parameter of PointParameter{if}v. */
		const val GL_POINT_SPRITE_COORD_ORIGIN = 36000


		/** Accepted by the {@code param} parameter of PointParameter{if}v. */
		const val GL_LOWER_LEFT = 36001
		const val GL_UPPER_LEFT = 36002


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_BLEND_EQUATION_RGB = 32777
		const val GL_BLEND_EQUATION_ALPHA = 34877


		/** Accepted by the {@code pname} parameter of GetIntegerv. */
		const val GL_STENCIL_BACK_FUNC = 34816
		const val GL_STENCIL_BACK_FAIL = 34817
		const val GL_STENCIL_BACK_PASS_DEPTH_FAIL = 34818
		const val GL_STENCIL_BACK_PASS_DEPTH_PASS = 34819
		const val GL_STENCIL_BACK_REF = 36003
		const val GL_STENCIL_BACK_VALUE_MASK = 36004
		const val GL_STENCIL_BACK_WRITEMASK = 36005

		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_CURRENT_RASTER_SECONDARY_COLOR = 33887


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GLFLOATMAT2x3 = 35685
		const val GLFLOATMAT2x4 = 35686
		const val GLFLOATMAT3x2 = 35687
		const val GLFLOATMAT3x4 = 35688
		const val GLFLOATMAT4x2 = 35689
		const val GLFLOATMAT4x3 = 35690


		/**
		* Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, GetBufferParameteriv, and
		* GetBufferPointerv.
		*/
		const val GL_PIXEL_PACK_BUFFER = 35051
		const val GL_PIXEL_UNPACK_BUFFER = 35052


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_PIXEL_PACK_BUFFER_BINDING = 35053
		const val GL_PIXEL_UNPACK_BUFFER_BINDING = 35055


		/** Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, CopyTexImage2D. */
		const val GL_SRGB = 35904
		const val GL_SRGB8 = 35905
		const val GL_SRGB_ALPHA = 35906
		const val GL_SRGB8_ALPHA8 = 35907
		const val GL_SLUMINANCE_ALPHA = 35908
		const val GL_SLUMINANCE8_ALPHA8 = 35909
		const val GL_SLUMINANCE = 35910
		const val GL_SLUMINANCE8 = 35911
		const val GL_COMPRESSED_SRGB = 35912
		const val GL_COMPRESSED_SRGB_ALPHA = 35913
		const val GL_COMPRESSED_SLUMINANCE = 35914
		const val GL_COMPRESSED_SLUMINANCE_ALPHA = 35915


		/** GetTarget */
		const val GL_MAJOR_VERSION = 33307
		const val GL_MINOR_VERSION = 33308
		const val GL_NUM_EXTENSIONS = 33309
		const val GL_CONTEXT_FLAGS = 33310
		const val GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT = 1


		/** Renamed tokens. */
		const val GL_COMPARE_REF_TO_TEXTURE = GL_COMPARE_R_TO_TEXTURE
		const val GL_CLIP_DISTANCE0 = GL_CLIP_PLANE0
		const val GL_CLIP_DISTANCE1 = GL_CLIP_PLANE1
		const val GL_CLIP_DISTANCE2 = GL_CLIP_PLANE2
		const val GL_CLIP_DISTANCE3 = GL_CLIP_PLANE3
		const val GL_CLIP_DISTANCE4 = GL_CLIP_PLANE4
		const val GL_CLIP_DISTANCE5 = GL_CLIP_PLANE5
		const val GL_CLIP_DISTANCE6 = 12294
		const val GL_CLIP_DISTANCE7 = 12295
		const val GL_MAX_CLIP_DISTANCES = GL_MAX_CLIP_PLANES
		const val GL_MAX_VARYING_COMPONENTS = GL_MAX_VARYING_FLOATS


		/** Accepted by the {@code pname} parameters of GetVertexAttribdv, GetVertexAttribfv, GetVertexAttribiv, GetVertexAttribIuiv and GetVertexAttribIiv. */
		const val GL_VERTEX_ATTRIB_ARRAY_INTEGER = 35069


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GL_SAMPLER_1D_ARRAY = 36288
		const val GL_SAMPLER_2D_ARRAY = 36289
		const val GL_SAMPLER_1D_ARRAY_SHADOW = 36291
		const val GL_SAMPLER_2D_ARRAY_SHADOW = 36292
		const val GL_SAMPLER_CUBE_SHADOW = 36293
		const val GL_UNSIGNED_INT_VEC2 = 36294
		const val GL_UNSIGNED_INT_VEC3 = 36295
		const val GL_UNSIGNED_INT_VEC4 = 36296
		const val GL_INT_SAMPLER_1D = 36297
		const val GL_INT_SAMPLER_2D = 36298
		const val GL_INT_SAMPLER_3D = 36299
		const val GL_INT_SAMPLER_CUBE = 36300
		const val GL_INT_SAMPLER_1D_ARRAY = 36302
		const val GL_INT_SAMPLER_2D_ARRAY = 36303
		const val GL_UNSIGNED_INT_SAMPLER_1D = 36305
		const val GL_UNSIGNED_INT_SAMPLER_2D = 36306
		const val GL_UNSIGNED_INT_SAMPLER_3D = 36307
		const val GL_UNSIGNED_INT_SAMPLER_CUBE = 36308
		const val GL_UNSIGNED_INT_SAMPLER_1D_ARRAY = 36310
		const val GL_UNSIGNED_INT_SAMPLER_2D_ARRAY = 36311


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MIN_PROGRAM_TEXEL_OFFSET = 35076
		const val GL_MAX_PROGRAM_TEXEL_OFFSET = 35077


		/** Accepted by the {@code mode} parameter of BeginConditionalRender. */
		const val GL_QUERY_WAIT = 36371
		const val GL_QUERY_NO_WAIT = 36372
		const val GL_QUERY_BY_REGION_WAIT = 36373
		const val GL_QUERY_BY_REGION_NO_WAIT = 36374


		/** Accepted by the {@code access} parameter of MapBufferRange. */
		const val GL_MAP_READ_BIT = 1
		const val GL_MAP_WRITE_BIT = 2
		const val GL_MAP_INVALIDATE_RANGE_BIT = 4
		const val GL_MAP_INVALIDATE_BUFFER_BIT = 8
		const val GL_MAP_FLUSH_EXPLICIT_BIT = 16
		const val GL_MAP_UNSYNCHRONIZED_BIT = 32


		/** Accepted by the {@code pname} parameter of GetBufferParameteriv. */
		const val GL_BUFFER_ACCESS_FLAGS = 37151
		const val GL_BUFFER_MAP_LENGTH = 37152
		const val GL_BUFFER_MAP_OFFSET = 37153


		/** Accepted by the {@code target} parameter of ClampColor and the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_CLAMP_VERTEX_COLOR = 35098
		const val GL_CLAMP_FRAGMENT_COLOR = 35099
		const val GL_CLAMP_READ_COLOR = 35100


		/** Accepted by the {@code clamp} parameter of ClampColor. */
		const val GL_FIXED_ONLY = 35101


		/**
		* Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, CopyTexImage2D, and RenderbufferStorage, and
		* returned in the {@code data} parameter of GetTexLevelParameter and GetRenderbufferParameteriv.
		*/
		const val GL_DEPTH_COMPONENT32F = 36012
		const val GL_DEPTH32F_STENCIL8 = 36013


		/**
		* Accepted by the {@code type} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, TexImage3D, TexSubImage1D, TexSubImage2D, TexSubImage3D, and
		* GetTexImage.
		*/
		const val GL_FLOAT_32_UNSIGNED_INT_24_8_REV = 36269


		/** Accepted by the {@code value} parameter of GetTexLevelParameter. */
		const val GL_TEXTURE_RED_TYPE = 35856
		const val GL_TEXTURE_GREEN_TYPE = 35857
		const val GL_TEXTURE_BLUE_TYPE = 35858
		const val GL_TEXTURE_ALPHA_TYPE = 35859
		const val GL_TEXTURE_LUMINANCE_TYPE = 35860
		const val GL_TEXTURE_INTENSITY_TYPE = 35861
		const val GL_TEXTURE_DEPTH_TYPE = 35862


		/** Returned by the {@code params} parameter of GetTexLevelParameter. */
		const val GL_UNSIGNED_NORMALIZED = 35863


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, and TexImage3D. */
		const val GL_RGBA32F = 34836
		const val GL_RGB32F = 34837
		const val GL_RGBA16F = 34842
		const val GL_RGB16F = 34843


		/** Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, CopyTexImage2D, and RenderbufferStorage. */
		const val GL_R11F_G11F_B10F = 35898


		/**
		* Accepted by the {@code type} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, GetTexImage, TexImage3D, TexSubImage1D, TexSubImage2D,
		* TexSubImage3D, GetHistogram, GetMinmax, ConvolutionFilter1D, ConvolutionFilter2D, ConvolutionFilter3D, GetConvolutionFilter, SeparableFilter2D,
		* GetSeparableFilter, ColorTable, ColorSubTable, and GetColorTable.
		*/
		const val GL_UNSIGNED_INT_10F_11F_11F_REV = 35899


		/** Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, CopyTexImage2D, and RenderbufferStorage. */
		const val GL_RGB9_E5 = 35901


		/**
		* Accepted by the {@code type} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, GetTexImage, TexImage3D, TexSubImage1D, TexSubImage2D,
		* TexSubImage3D, GetHistogram, GetMinmax, ConvolutionFilter1D, ConvolutionFilter2D, ConvolutionFilter3D, GetConvolutionFilter, SeparableFilter2D,
		* GetSeparableFilter, ColorTable, ColorSubTable, and GetColorTable.
		*/
		const val GL_UNSIGNED_INT_5_9_9_9_REV = 35902


		/** Accepted by the {@code pname} parameter of GetTexLevelParameterfv and GetTexLevelParameteriv. */
		const val GL_TEXTURE_SHARED_SIZE = 35903


		/**
		* Accepted by the {@code target} parameter of BindFramebuffer, CheckFramebufferStatus, FramebufferTexture{1D|2D|3D}, FramebufferRenderbuffer, and
		* GetFramebufferAttachmentParameteriv.
		*/
		const val GL_FRAMEBUFFER = 36160
		const val GL_READ_FRAMEBUFFER = 36008
		const val GL_DRAW_FRAMEBUFFER = 36009


		/**
		* Accepted by the {@code target} parameter of BindRenderbuffer, RenderbufferStorage, and GetRenderbufferParameteriv, and returned by
		* GetFramebufferAttachmentParameteriv.
		*/
		const val GL_RENDERBUFFER = 36161


		/** Accepted by the {@code internalformat} parameter of RenderbufferStorage. */
		const val GL_STENCIL_INDEX1 = 36166
		const val GL_STENCIL_INDEX4 = 36167
		const val GL_STENCIL_INDEX8 = 36168
		const val GL_STENCIL_INDEX16 = 36169


		/** Accepted by the {@code pname} parameter of GetRenderbufferParameteriv. */
		const val GL_RENDERBUFFER_WIDTH = 36162
		const val GL_RENDERBUFFER_HEIGHT = 36163
		const val GL_RENDERBUFFER_INTERNAL_FORMAT = 36164
		const val GL_RENDERBUFFER_RED_SIZE = 36176
		const val GL_RENDERBUFFER_GREEN_SIZE = 36177
		const val GL_RENDERBUFFER_BLUE_SIZE = 36178
		const val GL_RENDERBUFFER_ALPHA_SIZE = 36179
		const val GL_RENDERBUFFER_DEPTH_SIZE = 36180
		const val GL_RENDERBUFFER_STENCIL_SIZE = 36181
		const val GL_RENDERBUFFER_SAMPLES = 36011


		/** Accepted by the {@code pname} parameter of GetFramebufferAttachmentParameteriv. */
		const val GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE = 36048
		const val GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME = 36049
		const val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL = 36050
		const val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE = 36051
		const val GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER = 36052
		const val GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING = 33296
		const val GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE = 33297
		const val GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE = 33298
		const val GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE = 33299
		const val GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE = 33300
		const val GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE = 33301
		const val GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE = 33302
		const val GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE = 33303


		/** Returned in {@code params} by GetFramebufferAttachmentParameteriv. */
		const val GL_FRAMEBUFFER_DEFAULT = 33304
		const val GL_INDEX = 33314


		/** Accepted by the {@code attachment} parameter of FramebufferTexture{1D|2D|3D}, FramebufferRenderbuffer, and GetFramebufferAttachmentParameteriv. */
		const val GL_COLOR_ATTACHMENT0 = 36064
		const val GL_COLOR_ATTACHMENT1 = 36065
		const val GL_COLOR_ATTACHMENT2 = 36066
		const val GL_COLOR_ATTACHMENT3 = 36067
		const val GL_COLOR_ATTACHMENT4 = 36068
		const val GL_COLOR_ATTACHMENT5 = 36069
		const val GL_COLOR_ATTACHMENT6 = 36070
		const val GL_COLOR_ATTACHMENT7 = 36071
		const val GL_COLOR_ATTACHMENT8 = 36072
		const val GL_COLOR_ATTACHMENT9 = 36073
		const val GL_COLOR_ATTACHMENT10 = 36074
		const val GL_COLOR_ATTACHMENT11 = 36075
		const val GL_COLOR_ATTACHMENT12 = 36076
		const val GL_COLOR_ATTACHMENT13 = 36077
		const val GL_COLOR_ATTACHMENT14 = 36078
		const val GL_COLOR_ATTACHMENT15 = 36079
		const val GL_COLOR_ATTACHMENT16 = 36080
		const val GL_COLOR_ATTACHMENT17 = 36081
		const val GL_COLOR_ATTACHMENT18 = 36082
		const val GL_COLOR_ATTACHMENT19 = 36083
		const val GL_COLOR_ATTACHMENT20 = 36084
		const val GL_COLOR_ATTACHMENT21 = 36085
		const val GL_COLOR_ATTACHMENT22 = 36086
		const val GL_COLOR_ATTACHMENT23 = 36087
		const val GL_COLOR_ATTACHMENT24 = 36088
		const val GL_COLOR_ATTACHMENT25 = 36089
		const val GL_COLOR_ATTACHMENT26 = 36090
		const val GL_COLOR_ATTACHMENT27 = 36091
		const val GL_COLOR_ATTACHMENT28 = 36092
		const val GL_COLOR_ATTACHMENT29 = 36093
		const val GL_COLOR_ATTACHMENT30 = 36094
		const val GL_COLOR_ATTACHMENT31 = 36095
		const val GL_DEPTH_ATTACHMENT = 36096
		const val GL_STENCIL_ATTACHMENT = 36128
		const val GL_DEPTH_STENCIL_ATTACHMENT = 33306


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_SAMPLES = 36183


		/** Returned by CheckFramebufferStatus(). */
		const val GL_FRAMEBUFFER_COMPLETE = 36053
		const val GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 36054
		const val GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 36055
		const val GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 36059
		const val GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 36060
		const val GL_FRAMEBUFFER_UNSUPPORTED = 36061
		const val GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 36182
		const val GL_FRAMEBUFFER_UNDEFINED = 33305


		/** Accepted by the {@code pname} parameters of GetIntegerv, GetFloatv,  and GetDoublev. */
		const val GL_FRAMEBUFFER_BINDING = 36006
		const val GL_DRAW_FRAMEBUFFER_BINDING = 36006
		const val GL_READ_FRAMEBUFFER_BINDING = 36010
		const val GL_RENDERBUFFER_BINDING = 36007
		const val GL_MAX_COLOR_ATTACHMENTS = 36063
		const val GL_MAX_RENDERBUFFER_SIZE = 34024


		/** Returned by GetError(). */
		const val GL_INVALID_FRAMEBUFFER_OPERATION = 1286


		/**
		* Accepted by the {@code format} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, TexImage3D, TexSubImage1D, TexSubImage2D, TexSubImage3D, and
		* GetTexImage, by the {@code type} parameter of CopyPixels, by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D,
		* CopyTexImage2D, and RenderbufferStorage, and returned in the {@code data} parameter of GetTexLevelParameter and GetRenderbufferParameteriv.
		*/
		const val GL_DEPTH_STENCIL = 34041


		/**
		* Accepted by the {@code type} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, TexImage3D, TexSubImage1D, TexSubImage2D, TexSubImage3D, and
		* GetTexImage.
		*/
		const val GL_UNSIGNED_INT_24_8 = 34042


		/**
		* Accepted by the {@code internalformat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, CopyTexImage2D, and RenderbufferStorage, and
		* returned in the {@code data} parameter of GetTexLevelParameter and GetRenderbufferParameteriv.
		*/
		const val GL_DEPTH24_STENCIL8 = 35056


		/** Accepted by the {@code value} parameter of GetTexLevelParameter. */
		const val GL_TEXTURE_STENCIL_SIZE = 35057


		/**
		* Accepted by the {@code type} parameter of DrawPixels, ReadPixels, TexImage1D, TexImage2D, TexImage3D, GetTexImage, TexSubImage1D, TexSubImage2D,
		* TexSubImage3D, GetHistogram, GetMinmax, ConvolutionFilter1D, ConvolutionFilter2D, GetConvolutionFilter, SeparableFilter2D, GetSeparableFilter,
		* ColorTable, ColorSubTable, and GetColorTable.
		*
		* <p>Accepted by the {@code type} argument of VertexPointer, NormalPointer, ColorPointer, SecondaryColorPointer, FogCoordPointer, TexCoordPointer, and
		* VertexAttribPointer.</p>
		*/
		const val GL_HALF_FLOAT = 5131


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, and TexImage3D. */
		const val GL_RGBA32UI = 36208
		const val GL_RGB32UI = 36209
		const val GL_RGBA16UI = 36214
		const val GL_RGB16UI = 36215
		const val GL_RGBA8UI = 36220
		const val GL_RGB8UI = 36221
		const val GL_RGBA32I = 36226
		const val GL_RGB32I = 36227
		const val GL_RGBA16I = 36232
		const val GL_RGB16I = 36233
		const val GL_RGBA8I = 36238
		const val GL_RGB8I = 36239


		/** Accepted by the {@code format} parameter of TexImage1D, TexImage2D, TexImage3D, TexSubImage1D, TexSubImage2D, TexSubImage3D, DrawPixels and ReadPixels. */
		const val GL_RED_INTEGER = 36244
		const val GL_GREEN_INTEGER = 36245
		const val GL_BLUE_INTEGER = 36246
		const val GL_ALPHA_INTEGER = 36247
		const val GL_RGB_INTEGER = 36248
		const val GL_RGBA_INTEGER = 36249
		const val GL_BGR_INTEGER = 36250
		const val GL_BGRA_INTEGER = 36251


		/** Accepted by the {@code target} parameter of TexParameteri, TexParameteriv, TexParameterf, TexParameterfv, GenerateMipmap, and BindTexture. */
		const val GL_TEXTURE_1D_ARRAY = 35864
		const val GL_TEXTURE_2D_ARRAY = 35866


		/** Accepted by the {@code target} parameter of TexImage3D, TexSubImage3D, CopyTexSubImage3D, CompressedTexImage3D, and CompressedTexSubImage3D. */
		const val GL_PROXY_TEXTURE_2D_ARRAY = 35867


		/**
		* Accepted by the {@code target} parameter of TexImage2D, TexSubImage2D, CopyTexImage2D, CopyTexSubImage2D, CompressedTexImage2D, and
		* CompressedTexSubImage2D.
		*/
		const val GL_PROXY_TEXTURE_1D_ARRAY = 35865


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv and GetFloatv. */
		const val GL_TEXTURE_BINDING_1D_ARRAY = 35868
		const val GL_TEXTURE_BINDING_2D_ARRAY = 35869
		const val GL_MAX_ARRAY_TEXTURE_LAYERS = 35071


		/**
		* Accepted by the {@code internalformat} parameter of TexImage2D, CopyTexImage2D, and CompressedTexImage2D and the {@code format} parameter of
		* CompressedTexSubImage2D.
		*/
		const val GL_COMPRESSED_RED_RGTC1 = 36283
		const val GL_COMPRESSED_SIGNED_RED_RGTC1 = 36284
		const val GL_COMPRESSED_RG_RGTC2 = 36285
		const val GL_COMPRESSED_SIGNED_RG_RGTC2 = 36286


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, and CopyTexImage2D. */
		const val GL_R8 = 33321
		const val GL_R16 = 33322
		const val GL_RG8 = 33323
		const val GL_RG16 = 33324
		const val GL_R16F = 33325
		const val GL_R32F = 33326
		const val GL_RG16F = 33327
		const val GL_RG32F = 33328
		const val GL_R8I = 33329
		const val GL_R8UI = 33330
		const val GL_R16I = 33331
		const val GL_R16UI = 33332
		const val GL_R32I = 33333
		const val GL_R32UI = 33334
		const val GL_RG8I = 33335
		const val GL_RG8UI = 33336
		const val GL_RG16I = 33337
		const val GL_RG16UI = 33338
		const val GL_RG32I = 33339
		const val GL_RG32UI = 33340
		const val GL_RG = 33319
		const val GL_COMPRESSED_RED = 33317
		const val GL_COMPRESSED_RG = 33318


		/** Accepted by the {@code format} parameter of TexImage3D, TexImage2D, TexImage3D, TexSubImage1D, TexSubImage2D, TexSubImage3D, and ReadPixels. */
		const val GL_RG_INTEGER = 33320


		/**
		* Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, GetBufferPointerv,
		* BindBufferRange, BindBufferOffset and BindBufferBase.
		*/
		const val GL_TRANSFORM_FEEDBACK_BUFFER = 35982


		/** Accepted by the {@code param} parameter of GetIntegeri_v and GetBooleani_v. */
		const val GL_TRANSFORM_FEEDBACK_BUFFER_START = 35972
		const val GL_TRANSFORM_FEEDBACK_BUFFER_SIZE = 35973


		/**
		* Accepted by the {@code param} parameter of GetIntegeri_v and GetBooleani_v, and by the {@code pname} parameter of GetBooleanv,
		* GetDoublev, GetIntegerv, and GetFloatv.
		*/
		const val GL_TRANSFORM_FEEDBACK_BUFFER_BINDING = 35983


		/** Accepted by the {@code bufferMode} parameter of TransformFeedbackVaryings. */
		const val GL_INTERLEAVED_ATTRIBS = 35980
		const val GL_SEPARATE_ATTRIBS = 35981


		/** Accepted by the {@code target} parameter of BeginQuery, EndQuery, and GetQueryiv. */
		const val GL_PRIMITIVES_GENERATED = 35975
		const val GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 35976


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_RASTERIZER_DISCARD = 35977


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS = 35978
		const val GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS = 35979
		const val GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS = 35968


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_TRANSFORM_FEEDBACK_VARYINGS = 35971
		const val GL_TRANSFORM_FEEDBACK_BUFFER_MODE = 35967
		const val GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH = 35958


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_VERTEX_ARRAY_BINDING = 34229


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_FRAMEBUFFER_SRGB = 36281


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, and TexImage3D. */
		const val GL_R8_SNORM = 36756
		const val GL_RG8_SNORM = 36757
		const val GL_RGB8_SNORM = 36758
		const val GL_RGBA8_SNORM = 36759
		const val GL_R16_SNORM = 36760
		const val GL_RG16_SNORM = 36761
		const val GL_RGB16_SNORM = 36762
		const val GL_RGBA16_SNORM = 36763


		/** Returned by GetTexLevelParameter and GetFramebufferAttachmentParameter. */
		const val GL_SIGNED_NORMALIZED = 36764


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GL_SAMPLER_BUFFER = 36290
		const val GL_INT_SAMPLER_2D_RECT = 36301
		const val GL_INT_SAMPLER_BUFFER = 36304
		const val GL_UNSIGNED_INT_SAMPLER_2D_RECT = 36309
		const val GL_UNSIGNED_INT_SAMPLER_BUFFER = 36312


		/**
		* Accepted by the target parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, GetBufferPointerv, MapBufferRange,
		* FlushMappedBufferRange, GetBufferParameteriv, BindBufferRange, BindBufferBase, and CopyBufferSubData.
		*/
		const val GL_COPY_READ_BUFFER = 36662
		const val GL_COPY_WRITE_BUFFER = 36663


		/** Accepted by the {@code cap} parameter of Enable, Disable and IsEnabled. */
		const val GL_PRIMITIVE_RESTART = 36765


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_PRIMITIVE_RESTART_INDEX = 36766


		/**
		* Accepted by the {@code target} parameter of BindBuffer, BufferData, BufferSubData, MapBuffer, MapBufferRange, BindTexture, UnmapBuffer,
		* GetBufferSubData, GetBufferParameteriv, GetBufferPointerv, and TexBuffer, and the {@code pname} parameter of GetBooleanv, GetDoublev, GetFloatv, and
		* GetIntegerv.
		*/
		const val GL_TEXTURE_BUFFER = 35882


		/** Accepted by the {@code pname} parameters of GetBooleanv, GetDoublev, GetFloatv, and GetIntegerv. */
		const val GL_MAX_TEXTURE_BUFFER_SIZE = 35883
		const val GL_TEXTURE_BINDING_BUFFER = 35884
		const val GL_TEXTURE_BUFFER_DATA_STORE_BINDING = 35885


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable and IsEnabled; by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv and
		* GetDoublev; and by the {@code target} parameter of BindTexture, GetTexParameterfv, GetTexParameteriv, TexParameterf, TexParameteri, TexParameterfv and
		* TexParameteriv.
		*/
		const val GL_TEXTURE_RECTANGLE = 34037


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv and GetDoublev. */
		const val GL_TEXTURE_BINDING_RECTANGLE = 34038


		/** Accepted by the {@code target} parameter of GetTexLevelParameteriv, GetTexLevelParameterfv, GetTexParameteriv and TexImage2D. */
		const val GL_PROXY_TEXTURE_RECTANGLE = 34039


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv and GetFloatv. */
		const val GL_MAX_RECTANGLE_TEXTURE_SIZE = 34040


		/** Returned by {@code type} parameter of GetActiveUniform when the location {@code index} for program object {@code program} is of type sampler2DRect. */
		const val GL_SAMPLER_2D_RECT = 35683


		/**
		* Returned by {@code type} parameter of GetActiveUniform when the location {@code index} for program object {@code program} is of type
		* sampler2DRectShadow.
		*/
		const val GL_SAMPLER_2D_RECT_SHADOW = 35684


		/** Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, and GetBufferPointerv. */
		const val GL_UNIFORM_BUFFER = 35345


		/** Accepted by the {@code pname} parameter of GetIntegeri_v, GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_UNIFORM_BUFFER_BINDING = 35368


		/** Accepted by the {@code pname} parameter of GetIntegeri_v. */
		const val GL_UNIFORM_BUFFER_START = 35369
		const val GL_UNIFORM_BUFFER_SIZE = 35370


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_VERTEX_UNIFORM_BLOCKS = 35371
		const val GL_MAX_GEOMETRY_UNIFORM_BLOCKS = 35372
		const val GL_MAX_FRAGMENT_UNIFORM_BLOCKS = 35373
		const val GL_MAX_COMBINED_UNIFORM_BLOCKS = 35374
		const val GL_MAX_UNIFORM_BUFFER_BINDINGS = 35375
		const val GL_MAX_UNIFORM_BLOCK_SIZE = 35376
		const val GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS = 35377
		const val GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS = 35378
		const val GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS = 35379
		const val GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 35380


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = 35381
		const val GL_ACTIVE_UNIFORM_BLOCKS = 35382


		/** Accepted by the {@code pname} parameter of GetActiveUniformsiv. */
		const val GL_UNIFORM_TYPE = 35383
		const val GL_UNIFORM_SIZE = 35384
		const val GL_UNIFORM_NAME_LENGTH = 35385
		const val GL_UNIFORM_BLOCK_INDEX = 35386
		const val GL_UNIFORM_OFFSET = 35387
		const val GL_UNIFORM_ARRAY_STRIDE = 35388
		const val GL_UNIFORM_MATRIX_STRIDE = 35389
		const val GL_UNIFORM_IS_ROW_MAJOR = 35390


		/** Accepted by the {@code pname} parameter of GetActiveUniformBlockiv. */
		const val GL_UNIFORM_BLOCK_BINDING = 35391
		const val GL_UNIFORM_BLOCK_DATA_SIZE = 35392
		const val GL_UNIFORM_BLOCK_NAME_LENGTH = 35393
		const val GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS = 35394
		const val GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES = 35395
		const val GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER = 35396
		const val GL_UNIFORM_BLOCK_REFERENCED_BY_GEOMETRY_SHADER = 35397
		const val GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER = 35398


		/** Accepted by the {@code pname} parameter of GetIntegerv. */
		const val GL_CONTEXT_PROFILE_MASK = 37158


		/** Context profile bits. */
		const val GL_CONTEXT_CORE_PROFILE_BIT = 1
		const val GL_CONTEXT_COMPATIBILITY_PROFILE_BIT = 2


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_VERTEX_OUTPUT_COMPONENTS = 37154
		const val GL_MAX_GEOMETRY_INPUT_COMPONENTS = 37155
		const val GL_MAX_GEOMETRY_OUTPUT_COMPONENTS = 37156
		const val GL_MAX_FRAGMENT_INPUT_COMPONENTS = 37157


		/** Accepted by the {@code mode} parameter of ProvokingVertex. */
		const val GL_FIRST_VERTEX_CONVENTION = 36429
		const val GL_LAST_VERTEX_CONVENTION = 36430


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_PROVOKING_VERTEX = 36431
		const val GL_QUADS_FOLLOW_PROVOKING_VERTEX_CONVENTION = 36428


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv and
		* GetDoublev.
		*/
		const val GL_TEXTURE_CUBE_MAP_SEAMLESS = 34895


		/** Accepted by the {@code pname} parameter of GetMultisamplefv. */
		const val GL_SAMPLE_POSITION = 36432


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_SAMPLE_MASK = 36433


		/** Accepted by the {@code target} parameter of GetBooleani_v and GetIntegeri_v. */
		const val GL_SAMPLE_MASK_VALUE = 36434


		/** Accepted by the {@code target} parameter of BindTexture and TexImage2DMultisample. */
		const val GL_TEXTURE_2D_MULTISAMPLE = 37120


		/** Accepted by the {@code target} parameter of TexImage2DMultisample. */
		const val GL_PROXY_TEXTURE_2D_MULTISAMPLE = 37121


		/** Accepted by the {@code target} parameter of BindTexture and TexImage3DMultisample. */
		const val GL_TEXTURE_2D_MULTISAMPLE_ARRAY = 37122


		/** Accepted by the {@code target} parameter of TexImage3DMultisample. */
		const val GL_PROXY_TEXTURE_2D_MULTISAMPLE_ARRAY = 37123


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_MAX_SAMPLE_MASK_WORDS = 36441
		const val GL_MAX_COLOR_TEXTURE_SAMPLES = 37134
		const val GL_MAX_DEPTH_TEXTURE_SAMPLES = 37135
		const val GL_MAX_INTEGER_SAMPLES = 37136
		const val GL_TEXTURE_BINDING_2D_MULTISAMPLE = 37124
		const val GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY = 37125


		/** Accepted by the {@code pname} parameter of GetTexLevelParameter. */
		const val GL_TEXTURE_SAMPLES = 37126
		const val GL_TEXTURE_FIXED_SAMPLE_LOCATIONS = 37127


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GL_SAMPLER_2D_MULTISAMPLE = 37128
		const val GL_INT_SAMPLER_2D_MULTISAMPLE = 37129
		const val GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE = 37130
		const val GL_SAMPLER_2D_MULTISAMPLE_ARRAY = 37131
		const val GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY = 37132
		const val GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY = 37133


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_DEPTH_CLAMP = 34383


		/** Accepted by the {@code type} parameter of CreateShader and returned by the {@code params} parameter of GetShaderiv. */
		const val GL_GEOMETRY_SHADER = 36313


		/** Accepted by the {@code pname} parameter of ProgramParameteri and GetProgramiv. */
		const val GL_GEOMETRY_VERTICES_OUT = 36314
		const val GL_GEOMETRY_INPUT_TYPE = 36315
		const val GL_GEOMETRY_OUTPUT_TYPE = 36316


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS = 35881
		const val GL_MAX_GEOMETRY_UNIFORM_COMPONENTS = 36319
		const val GL_MAX_GEOMETRY_OUTPUT_VERTICES = 36320
		const val GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS = 36321


		/** Accepted by the {@code mode} parameter of Begin, DrawArrays, MultiDrawArrays, DrawElements, MultiDrawElements, and DrawRangeElements. */
		const val GL_LINES_ADJACENCY = 10
		const val GL_LINE_STRIP_ADJACENCY = 11
		const val GL_TRIANGLES_ADJACENCY = 12
		const val GL_TRIANGLE_STRIP_ADJACENCY = 13


		/** Returned by CheckFramebufferStatus. */
		const val GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS = 36264


		/** Accepted by the {@code pname} parameter of GetFramebufferAttachment- Parameteriv. */
		const val GL_FRAMEBUFFER_ATTACHMENT_LAYERED = 36263


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetIntegerv, GetFloatv, GetDoublev, and
		* GetBooleanv.
		*/
		const val GL_PROGRAM_POINT_SIZE = 34370


		/** Accepted as the {@code pname} parameter of GetInteger64v. */
		const val GL_MAX_SERVER_WAIT_TIMEOUT = 37137


		/** Accepted as the {@code pname} parameter of GetSynciv. */
		const val GL_OBJECT_TYPE = 37138
		const val GL_SYNC_CONDITION = 37139
		const val GL_SYNC_STATUS = 37140
		const val GL_SYNC_FLAGS = 37141


		/** Returned in {@code values} for GetSynciv {@code pname} OBJECT_TYPE. */
		const val GL_SYNC_FENCE = 37142


		/** Returned in {@code values} for GetSynciv {@code pname} SYNC_CONDITION. */
		const val GL_SYNC_GPU_COMMANDS_COMPLETE = 37143


		/** Returned in {@code values} for GetSynciv {@code pname} SYNC_STATUS. */
		const val GL_UNSIGNALED = 37144
		const val GL_SIGNALED = 37145


		/** Accepted in the {@code flags} parameter of ClientWaitSync. */
		const val GL_SYNC_FLUSH_COMMANDS_BIT = 1


		/** Accepted in the {@code timeout} parameter of WaitSync. */
		const val GL_TIMEOUT_IGNORED = -1L

		/** Returned by ClientWaitSync. */
		const val GL_ALREADY_SIGNALED = 37146
		const val GL_TIMEOUT_EXPIRED = 37147
		const val GL_CONDITION_SATISFIED = 37148
		const val GL_WAIT_FAILED = 37149


		/**
		* Accepted by the {@code src} and {@code dst} parameters of BlendFunc and BlendFunci, and by the {@code srcRGB}, {@code dstRGB}, {@code srcAlpha} and
		* {@code dstAlpha} parameters of BlendFuncSeparate and BlendFuncSeparatei.
		*/
		const val GL_SRC1_COLOR = 35065
		const val GL_ONE_MINUS_SRC1_COLOR = 35066
		const val GL_ONE_MINUS_SRC1_ALPHA = 35067


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv and GetDoublev. */
		const val GL_MAX_DUAL_SOURCE_DRAW_BUFFERS = 35068


		/** Accepted by the {@code target} parameter of BeginQuery, EndQuery, and GetQueryiv. */
		const val GL_ANY_SAMPLES_PASSED = 35887


		/** Accepted by the {@code value} parameter of the GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv and GetDoublev functions. */
		const val GL_SAMPLER_BINDING = 35097


		/**
		* Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, TexImage3D, CopyTexImage1D, CopyTexImage2D, RenderbufferStorage and
		* RenderbufferStorageMultisample.
		*/
		const val GL_RGB10_A2UI = 36975


		/** Accepted by the {@code pname} parameters of TexParameteri, TexParameterf, TexParameteriv, TexParameterfv, GetTexParameterfv, and GetTexParameteriv. */
		const val GL_TEXTURE_SWIZZLE_R = 36418
		const val GL_TEXTURE_SWIZZLE_G = 36419
		const val GL_TEXTURE_SWIZZLE_B = 36420
		const val GL_TEXTURE_SWIZZLE_A = 36421


		/** Accepted by the {@code pname} parameters of TexParameteriv,  TexParameterfv, GetTexParameterfv, and GetTexParameteriv. */
		const val GL_TEXTURE_SWIZZLE_RGBA = 36422


		/** Accepted by the {@code target} parameter of BeginQuery, EndQuery, and GetQueryiv. */
		const val GL_TIME_ELAPSED = 35007


		/**
		* Accepted by the {@code target} parameter of GetQueryiv and QueryCounter. Accepted by the {@code value} parameter of GetBooleanv, GetIntegerv,
		* GetInteger64v, GetFloatv, and GetDoublev.
		*/
		const val GL_TIMESTAMP = 36392


		/** Accepted by the {@code pname} parameters of GetVertexAttribdv, GetVertexAttribfv, and GetVertexAttribiv. */
		const val GL_VERTEX_ATTRIB_ARRAY_DIVISOR = 35070


		/**
		* Accepted by the {@code type} parameter of VertexAttribPointer, VertexPointer, NormalPointer, ColorPointer, SecondaryColorPointer, TexCoordPointer,
		* VertexAttribP{1234}ui, VertexP*, TexCoordP*, MultiTexCoordP*, NormalP3ui, ColorP*, SecondaryColorP* and VertexAttribP*.
		*/
		const val GL_INT_2_10_10_10_REV = 36255


		/**
		* Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, GetBufferPointerv,
		* MapBufferRange, FlushMappedBufferRange, GetBufferParameteriv, and CopyBufferSubData.
		*/
		const val GL_DRAW_INDIRECT_BUFFER = 36671


		/** Accepted by the {@code value} parameter of GetIntegerv, GetBooleanv, GetFloatv, and GetDoublev. */
		const val GL_DRAW_INDIRECT_BUFFER_BINDING = 36675


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_GEOMETRY_SHADER_INVOCATIONS = 34943


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, GetDoublev, and GetInteger64v. */
		const val GL_MAX_GEOMETRY_SHADER_INVOCATIONS = 36442
		const val GL_MIN_FRAGMENT_INTERPOLATION_OFFSET = 36443
		const val GL_MAX_FRAGMENT_INTERPOLATION_OFFSET = 36444
		const val GL_FRAGMENT_INTERPOLATION_OFFSET_BITS = 36445


		/** Returned in the {@code type} parameter of GetActiveUniform, and GetTransformFeedbackVarying. */
		const val GL_DOUBLE_VEC2 = 36860
		const val GL_DOUBLE_VEC3 = 36861
		const val GL_DOUBLE_VEC4 = 36862
		const val GL_DOUBLE_MAT2 = 36678
		const val GL_DOUBLE_MAT3 = 36679
		const val GL_DOUBLE_MAT4 = 36680
		const val GLDOUBLEMAT2x3 = 36681
		const val GLDOUBLEMAT2x4 = 36682
		const val GLDOUBLEMAT3x2 = 36683
		const val GLDOUBLEMAT3x4 = 36684
		const val GLDOUBLEMAT4x2 = 36685
		const val GLDOUBLEMAT4x3 = 36686


		/**
		* Accepted by the {@code cap} parameter of Enable, Disable, and IsEnabled, and by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and
		* GetDoublev.
		*/
		const val GL_SAMPLE_SHADING = 35894


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_MIN_SAMPLE_SHADING_VALUE = 35895


		/** Accepted by the {@code pname} parameter of GetProgramStageiv. */
		const val GL_ACTIVE_SUBROUTINES = 36325
		const val GL_ACTIVE_SUBROUTINE_UNIFORMS = 36326
		const val GL_ACTIVE_SUBROUTINE_UNIFORM_LOCATIONS = 36423
		const val GL_ACTIVE_SUBROUTINE_MAX_LENGTH = 36424
		const val GL_ACTIVE_SUBROUTINE_UNIFORM_MAX_LENGTH = 36425


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, GetDoublev, and GetInteger64v. */
		const val GL_MAX_SUBROUTINES = 36327
		const val GL_MAX_SUBROUTINE_UNIFORM_LOCATIONS = 36328


		/** Accepted by the {@code pname} parameter of GetActiveSubroutineUniformiv. */
		const val GL_NUM_COMPATIBLE_SUBROUTINES = 36426
		const val GL_COMPATIBLE_SUBROUTINES = 36427


		/** Accepted by the {@code mode} parameter of Begin and all vertex array functions that implicitly call Begin. */
		const val GL_PATCHES = 14


		/** Accepted by the {@code pname} parameter of PatchParameteri, GetBooleanv, GetDoublev, GetFloatv, GetIntegerv, and GetInteger64v. */
		const val GL_PATCH_VERTICES = 36466


		/** Accepted by the {@code pname} parameter of PatchParameterfv, GetBooleanv, GetDoublev, GetFloatv, and GetIntegerv, and GetInteger64v. */
		const val GL_PATCH_DEFAULT_INNER_LEVEL = 36467
		const val GL_PATCH_DEFAULT_OUTER_LEVEL = 36468


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_TESS_CONTROL_OUTPUT_VERTICES = 36469
		const val GL_TESS_GEN_MODE = 36470
		const val GL_TESS_GEN_SPACING = 36471
		const val GL_TESS_GEN_VERTEX_ORDER = 36472
		const val GL_TESS_GEN_POINT_MODE = 36473


		/** Returned by GetProgramiv when {@code pname} is TESS_GEN_MODE. */
		const val GL_ISOLINES = 36474


		/** Returned by GetProgramiv when {@code pname} is TESS_GEN_SPACING. */
		const val GL_FRACTIONAL_ODD = 36475
		const val GL_FRACTIONAL_EVEN = 36476


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetFloatv, GetIntegerv, and GetInteger64v. */
		const val GL_MAX_PATCH_VERTICES = 36477
		const val GL_MAX_TESS_GEN_LEVEL = 36478
		const val GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS = 36479
		const val GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS = 36480
		const val GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS = 36481
		const val GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS = 36482
		const val GL_MAX_TESS_CONTROL_OUTPUT_COMPONENTS = 36483
		const val GL_MAX_TESS_PATCH_COMPONENTS = 36484
		const val GL_MAX_TESS_CONTROL_TOTAL_OUTPUT_COMPONENTS = 36485
		const val GL_MAX_TESS_EVALUATION_OUTPUT_COMPONENTS = 36486
		const val GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS = 36489
		const val GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS = 36490
		const val GL_MAX_TESS_CONTROL_INPUT_COMPONENTS = 34924
		const val GL_MAX_TESS_EVALUATION_INPUT_COMPONENTS = 34925
		const val GL_MAX_COMBINED_TESS_CONTROL_UNIFORM_COMPONENTS = 36382
		const val GL_MAX_COMBINED_TESS_EVALUATION_UNIFORM_COMPONENTS = 36383


		/** Accepted by the {@code pname} parameter of GetActiveUniformBlockiv. */
		const val GL_UNIFORM_BLOCK_REFERENCED_BY_TESS_CONTROL_SHADER = 34032
		const val GL_UNIFORM_BLOCK_REFERENCED_BY_TESS_EVALUATION_SHADER = 34033


		/** Accepted by the {@code type} parameter of CreateShader and returned by the {@code params} parameter of GetShaderiv. */
		const val GL_TESS_EVALUATION_SHADER = 36487
		const val GL_TESS_CONTROL_SHADER = 36488


		/** Accepted by the {@code target} parameter of TexParameteri, TexParameteriv, TexParameterf, TexParameterfv, BindTexture, and GenerateMipmap. */
		const val GL_TEXTURE_CUBE_MAP_ARRAY = 36873


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv and GetFloatv. */
		const val GL_TEXTURE_BINDING_CUBE_MAP_ARRAY = 36874


		/** Accepted by the {@code target} parameter of TexImage3D, TexSubImage3D, CompressedTeximage3D, CompressedTexSubImage3D and CopyTexSubImage3D. */
		const val GL_PROXY_TEXTURE_CUBE_MAP_ARRAY = 36875


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GL_SAMPLER_CUBE_MAP_ARRAY = 36876
		const val GL_SAMPLER_CUBE_MAP_ARRAY_SHADOW = 36877
		const val GL_INT_SAMPLER_CUBE_MAP_ARRAY = 36878
		const val GL_UNSIGNED_INT_SAMPLER_CUBE_MAP_ARRAY = 36879


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_MIN_PROGRAM_TEXTURE_GATHER_OFFSET = 36446
		const val GL_MAX_PROGRAM_TEXTURE_GATHER_OFFSET = 36447


		/** Accepted by the {@code target} parameter of BindTransformFeedback. */
		const val GL_TRANSFORM_FEEDBACK = 36386


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_TRANSFORM_FEEDBACK_BUFFER_PAUSED = 36387
		const val GL_TRANSFORM_FEEDBACK_BUFFER_ACTIVE = 36388
		const val GL_TRANSFORM_FEEDBACK_BINDING = 36389


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetIntegerv, and GetFloatv. */
		const val GL_MAX_TRANSFORM_FEEDBACK_BUFFERS = 36464
		const val GL_MAX_VERTEX_STREAMS = 36465



		/** Accepted by the {@code value} parameter of GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_SHADER_COMPILER = 36346
		const val GL_SHADER_BINARY_FORMATS = 36344
		const val GL_NUM_SHADER_BINARY_FORMATS = 36345
		const val GL_MAX_VERTEX_UNIFORM_VECTORS = 36347
		const val GL_MAX_VARYING_VECTORS = 36348
		const val GL_MAX_FRAGMENT_UNIFORM_VECTORS = 36349
		const val GL_IMPLEMENTATION_COLOR_READ_TYPE = 35738
		const val GL_IMPLEMENTATION_COLOR_READ_FORMAT = 35739


		/** Accepted by the {@code type} parameter of VertexAttribPointer. */
		const val GL_FIXED = 5132


		/** Accepted by the {@code precisiontype} parameter of GetShaderPrecisionFormat. */
		const val GL_LOW_FLOAT = 36336
		const val GL_MEDIUM_FLOAT = 36337
		const val GL_HIGH_FLOAT = 36338
		const val GL_LOW_INT = 36339
		const val GL_MEDIUM_INT = 36340
		const val GL_HIGH_INT = 36341


		/** Accepted by the {@code format} parameter of most commands taking sized internal formats. */
		const val GL_RGB565 = 36194


		/** Accepted by the {@code pname} parameter of ProgramParameteri and GetProgramiv. */
		const val GL_PROGRAM_BINARY_RETRIEVABLE_HINT = 33367


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_PROGRAM_BINARY_LENGTH = 34625


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv and GetDoublev. */
		const val GL_NUM_PROGRAM_BINARY_FORMATS = 34814
		const val GL_PROGRAM_BINARY_FORMATS = 34815


		/** Accepted by {@code stages} parameter to UseProgramStages. */
		const val GL_VERTEX_SHADER_BIT = 1
		const val GL_FRAGMENT_SHADER_BIT = 2
		const val GL_GEOMETRY_SHADER_BIT = 4
		const val GL_TESS_CONTROL_SHADER_BIT = 8
		const val GL_TESS_EVALUATION_SHADER_BIT = 16
		const val GL_ALL_SHADER_BITS = -1


		/** Accepted by the {@code pname} parameter of ProgramParameteri and GetProgramiv. */
		const val GL_PROGRAM_SEPARABLE = 33368


		/** Accepted by {@code type} parameter to GetProgramPipelineiv. */
		const val GL_ACTIVE_PROGRAM = 33369


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_PROGRAM_PIPELINE_BINDING = 33370


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, GetDoublev and GetInteger64v. */
		const val GL_MAX_VIEWPORTS = 33371
		const val GL_VIEWPORT_SUBPIXEL_BITS = 33372
		const val GL_VIEWPORT_BOUNDS_RANGE = 33373
		const val GL_LAYER_PROVOKING_VERTEX = 33374
		const val GL_VIEWPORT_INDEX_PROVOKING_VERTEX = 33375


		/** Returned in the {@code data} parameter from a Get query with a {@code pname} of LAYER_PROVOKING_VERTEX or VIEWPORT_INDEX_PROVOKING_VERTEX. */
		const val GL_UNDEFINED_VERTEX = 33376


		/** Renamed tokens. */
		const val GL_COPY_READ_BUFFER_BINDING = GL_COPY_READ_BUFFER
		const val GL_COPY_WRITE_BUFFER_BINDING = GL_COPY_WRITE_BUFFER
		const val GL_TRANSFORM_FEEDBACK_ACTIVE = GL_TRANSFORM_FEEDBACK_BUFFER_ACTIVE
		const val GL_TRANSFORM_FEEDBACK_PAUSED = GL_TRANSFORM_FEEDBACK_BUFFER_PAUSED


		/**
		* Accepted by the {@code internalformat} parameter of TexImage2D, TexImage3D, CopyTexImage2D, CopyTexImage3D, CompressedTexImage2D, and
		* CompressedTexImage3D and the {@code format} parameter of CompressedTexSubImage2D and CompressedTexSubImage3D.
		*/
		const val GL_COMPRESSED_RGBA_BPTC_UNORM = 36492
		const val GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM = 36493
		const val GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT = 36494
		const val GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = 36495


		/** Accepted by the {@code pname} parameter of PixelStore[fi], GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_UNPACK_COMPRESSED_BLOCK_WIDTH = 37159
		const val GL_UNPACK_COMPRESSED_BLOCK_HEIGHT = 37160
		const val GL_UNPACK_COMPRESSED_BLOCK_DEPTH = 37161
		const val GL_UNPACK_COMPRESSED_BLOCK_SIZE = 37162
		const val GL_PACK_COMPRESSED_BLOCK_WIDTH = 37163
		const val GL_PACK_COMPRESSED_BLOCK_HEIGHT = 37164
		const val GL_PACK_COMPRESSED_BLOCK_DEPTH = 37165
		const val GL_PACK_COMPRESSED_BLOCK_SIZE = 37166


		/** Accepted by the {@code target} parameter of BindBufferBase and BindBufferRange. */
		const val GL_ATOMIC_COUNTER_BUFFER = 37568


		/**
		* Accepted by the {@code pname} parameter of GetBooleani_v, GetIntegeri_v, GetFloati_v, GetDoublei_v, GetInteger64i_v, GetBooleanv, GetIntegerv,
		* GetInteger64v, GetFloatv, GetDoublev, and GetActiveAtomicCounterBufferiv.
		*/
		const val GL_ATOMIC_COUNTER_BUFFER_BINDING = 37569


		/** Accepted by the {@code pname} parameter of GetIntegeri_64v. */
		const val GL_ATOMIC_COUNTER_BUFFER_START = 37570
		const val GL_ATOMIC_COUNTER_BUFFER_SIZE = 37571


		/** Accepted by the {@code pname} parameter of GetActiveAtomicCounterBufferiv. */
		const val GL_ATOMIC_COUNTER_BUFFER_DATA_SIZE = 37572
		const val GL_ATOMIC_COUNTER_BUFFER_ACTIVE_ATOMIC_COUNTERS = 37573
		const val GL_ATOMIC_COUNTER_BUFFER_ACTIVE_ATOMIC_COUNTER_INDICES = 37574
		const val GL_ATOMIC_COUNTER_BUFFER_REFERENCED_BY_VERTEX_SHADER = 37575
		const val GL_ATOMIC_COUNTER_BUFFER_REFERENCED_BY_TESS_CONTROL_SHADER = 37576
		const val GL_ATOMIC_COUNTER_BUFFER_REFERENCED_BY_TESS_EVALUATION_SHADER = 37577
		const val GL_ATOMIC_COUNTER_BUFFER_REFERENCED_BY_GEOMETRY_SHADER = 37578
		const val GL_ATOMIC_COUNTER_BUFFER_REFERENCED_BY_FRAGMENT_SHADER = 37579


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_MAX_VERTEX_ATOMIC_COUNTER_BUFFERS = 37580
		const val GL_MAX_TESS_CONTROL_ATOMIC_COUNTER_BUFFERS = 37581
		const val GL_MAX_TESS_EVALUATION_ATOMIC_COUNTER_BUFFERS = 37582
		const val GL_MAX_GEOMETRY_ATOMIC_COUNTER_BUFFERS = 37583
		const val GL_MAX_FRAGMENT_ATOMIC_COUNTER_BUFFERS = 37584
		const val GL_MAX_COMBINED_ATOMIC_COUNTER_BUFFERS = 37585
		const val GL_MAX_VERTEX_ATOMIC_COUNTERS = 37586
		const val GL_MAX_TESS_CONTROL_ATOMIC_COUNTERS = 37587
		const val GL_MAX_TESS_EVALUATION_ATOMIC_COUNTERS = 37588
		const val GL_MAX_GEOMETRY_ATOMIC_COUNTERS = 37589
		const val GL_MAX_FRAGMENT_ATOMIC_COUNTERS = 37590
		const val GL_MAX_COMBINED_ATOMIC_COUNTERS = 37591
		const val GL_MAX_ATOMIC_COUNTER_BUFFER_SIZE = 37592
		const val GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS = 37596


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_ACTIVE_ATOMIC_COUNTER_BUFFERS = 37593


		/** Accepted by the {@code pname} parameter of GetActiveUniformsiv. */
		const val GL_UNIFORM_ATOMIC_COUNTER_BUFFER_INDEX = 37594


		/** Returned in {@code params} by GetActiveUniform and GetActiveUniformsiv. */
		const val GL_UNSIGNED_INT_ATOMIC_COUNTER = 37595


		/** Accepted by the {@code value} parameter of GetTexParameter{if}v. */
		const val GL_TEXTURE_IMMUTABLE_FORMAT = 37167


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, GetDoublev, and GetInteger64v. */
		const val GL_MAX_IMAGE_UNITS = 36664
		const val GL_MAX_COMBINED_IMAGE_UNITS_AND_FRAGMENT_OUTPUTS = 36665
		const val GL_MAX_IMAGE_SAMPLES = 36973
		const val GL_MAX_VERTEX_IMAGE_UNIFORMS = 37066
		const val GL_MAX_TESS_CONTROL_IMAGE_UNIFORMS = 37067
		const val GL_MAX_TESS_EVALUATION_IMAGE_UNIFORMS = 37068
		const val GL_MAX_GEOMETRY_IMAGE_UNIFORMS = 37069
		const val GL_MAX_FRAGMENT_IMAGE_UNIFORMS = 37070
		const val GL_MAX_COMBINED_IMAGE_UNIFORMS = 37071


		/** Accepted by the {@code target} parameter of GetIntegeri_v and GetBooleani_v. */
		const val GL_IMAGE_BINDING_NAME = 36666
		const val GL_IMAGE_BINDING_LEVEL = 36667
		const val GL_IMAGE_BINDING_LAYERED = 36668
		const val GL_IMAGE_BINDING_LAYER = 36669
		const val GL_IMAGE_BINDING_ACCESS = 36670
		const val GL_IMAGE_BINDING_FORMAT = 36974


		/** Accepted by the {@code barriers} parameter of MemoryBarrier. */
		const val GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 1
		const val GL_ELEMENT_ARRAY_BARRIER_BIT = 2
		const val GL_UNIFORM_BARRIER_BIT = 4
		const val GL_TEXTURE_FETCH_BARRIER_BIT = 8
		const val GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 32
		const val GL_COMMAND_BARRIER_BIT = 64
		const val GL_PIXEL_BUFFER_BARRIER_BIT = 128
		const val GL_TEXTURE_UPDATE_BARRIER_BIT = 256
		const val GL_BUFFER_UPDATE_BARRIER_BIT = 512
		const val GL_FRAMEBUFFER_BARRIER_BIT = 1024
		const val GL_TRANSFORM_FEEDBACK_BARRIER_BIT = 2048
		const val GL_ATOMIC_COUNTER_BARRIER_BIT = 4096
		const val GL_ALL_BARRIER_BITS = -1


		/** Returned by the {@code type} parameter of GetActiveUniform. */
		const val GL_IMAGE_1D = 36940
		const val GL_IMAGE_2D = 36941
		const val GL_IMAGE_3D = 36942
		const val GL_IMAGE_2D_RECT = 36943
		const val GL_IMAGE_CUBE = 36944
		const val GL_IMAGE_BUFFER = 36945
		const val GL_IMAGE_1D_ARRAY = 36946
		const val GL_IMAGE_2D_ARRAY = 36947
		const val GL_IMAGE_CUBE_MAP_ARRAY = 36948
		const val GL_IMAGE_2D_MULTISAMPLE = 36949
		const val GL_IMAGE_2D_MULTISAMPLE_ARRAY = 36950
		const val GL_INT_IMAGE_1D = 36951
		const val GL_INT_IMAGE_2D = 36952
		const val GL_INT_IMAGE_3D = 36953
		const val GL_INT_IMAGE_2D_RECT = 36954
		const val GL_INT_IMAGE_CUBE = 36955
		const val GL_INT_IMAGE_BUFFER = 36956
		const val GL_INT_IMAGE_1D_ARRAY = 36957
		const val GL_INT_IMAGE_2D_ARRAY = 36958
		const val GL_INT_IMAGE_CUBE_MAP_ARRAY = 36959
		const val GL_INT_IMAGE_2D_MULTISAMPLE = 36960
		const val GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY = 36961
		const val GL_UNSIGNED_INT_IMAGE_1D = 36962
		const val GL_UNSIGNED_INT_IMAGE_2D = 36963
		const val GL_UNSIGNED_INT_IMAGE_3D = 36964
		const val GL_UNSIGNED_INT_IMAGE_2D_RECT = 36965
		const val GL_UNSIGNED_INT_IMAGE_CUBE = 36966
		const val GL_UNSIGNED_INT_IMAGE_BUFFER = 36967
		const val GL_UNSIGNED_INT_IMAGE_1D_ARRAY = 36968
		const val GL_UNSIGNED_INT_IMAGE_2D_ARRAY = 36969
		const val GL_UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY = 36970
		const val GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE = 36971
		const val GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY = 36972


		/** Accepted by the {@code value} parameter of GetTexParameteriv, GetTexParameterfv, GetTexParameterIiv, and GetTexParameterIuiv. */
		const val GL_IMAGE_FORMAT_COMPATIBILITY_TYPE = 37063


		/**
		* Returned in the {@code data} parameter of GetTexParameteriv, GetTexParameterfv, GetTexParameterIiv, and GetTexParameterIuiv when {@code value} is
		* IMAGE_FORMAT_COMPATIBILITY_TYPE.
		*/
		const val GL_IMAGE_FORMAT_COMPATIBILITY_BY_SIZE = 37064
		const val GL_IMAGE_FORMAT_COMPATIBILITY_BY_CLASS = 37065


		/** Accepted by the {@code pname} parameter of GetInternalformativ. */
		const val GL_NUM_SAMPLE_COUNTS = 37760


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_MIN_MAP_BUFFER_ALIGNMENT = 37052


		/** No. of supported Shading Language Versions. Accepted by the {@code pname} parameter of GetIntegerv. */
		const val GL_NUM_SHADING_LANGUAGE_VERSIONS = 33513


		/** Vertex attrib array has unconverted doubles. Accepted by the {@code pname} parameter of GetVertexAttribiv. */
		const val GL_VERTEX_ATTRIB_ARRAY_LONG = 34638


		/** Accepted by the {@code internalformat} parameter of CompressedTexImage2D. */
		const val GL_COMPRESSED_RGB8_ETC2 = 37492
		const val GL_COMPRESSED_SRGB8_ETC2 = 37493
		const val GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 37494
		const val GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 37495
		const val GL_COMPRESSED_RGBA8_ETC2_EAC = 37496
		const val GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC = 37497
		const val GL_COMPRESSED_R11_EAC = 37488
		const val GL_COMPRESSED_SIGNED_R11_EAC = 37489
		const val GL_COMPRESSED_RG11_EAC = 37490
		const val GL_COMPRESSED_SIGNED_RG11_EAC = 37491


		/** Accepted by the {@code target} parameter of Enable and Disable. */
		const val GL_PRIMITIVE_RESTART_FIXED_INDEX = 36201


		/** Accepted by the {@code target} parameter of BeginQuery, EndQuery, GetQueryIndexediv and GetQueryiv. */
		const val GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 36202


		/** Accepted by the {@code value} parameter of the GetInteger* functions. */
		const val GL_MAX_ELEMENT_INDEX = 36203


		/** Accepted by the {@code pname} parameters of GetTexParameterfv and  GetTexParameteriv. */
		const val GL_TEXTURE_IMMUTABLE_LEVELS = 33503


		/** Accepted by the {@code type} parameter of CreateShader and returned in the {@code params} parameter by GetShaderiv. */
		const val GL_COMPUTE_SHADER = 37305


		/** Accepted by the {@code pname} parameter of GetIntegerv, GetBooleanv, GetFloatv, GetDoublev and GetInteger64v. */
		const val GL_MAX_COMPUTE_UNIFORM_BLOCKS = 37307
		const val GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS = 37308
		const val GL_MAX_COMPUTE_IMAGE_UNIFORMS = 37309
		const val GL_MAX_COMPUTE_SHARED_MEMORY_SIZE = 33378
		const val GL_MAX_COMPUTE_UNIFORM_COMPONENTS = 33379
		const val GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS = 33380
		const val GL_MAX_COMPUTE_ATOMIC_COUNTERS = 33381
		const val GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS = 33382
		const val GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = 37099


		/** Accepted by the {@code pname} parameter of GetIntegeri_v, GetBooleani_v, GetFloati_v, GetDoublei_v and GetInteger64i_v. */
		const val GL_MAX_COMPUTE_WORK_GROUP_COUNT = 37310
		const val GL_MAX_COMPUTE_WORK_GROUP_SIZE = 37311


		/** Accepted by the {@code pname} parameter of GetProgramiv. */
		const val GL_COMPUTE_WORK_GROUP_SIZE = 33383


		/** Accepted by the {@code pname} parameter of GetActiveUniformBlockiv. */
		const val GL_UNIFORM_BLOCK_REFERENCED_BY_COMPUTE_SHADER = 37100


		/** Accepted by the {@code pname} parameter of GetActiveAtomicCounterBufferiv. */
		const val GL_ATOMIC_COUNTER_BUFFER_REFERENCED_BY_COMPUTE_SHADER = 37101


		/** Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, and GetBufferPointerv. */
		const val GL_DISPATCH_INDIRECT_BUFFER = 37102


		/** Accepted by the {@code value} parameter of GetIntegerv, GetBooleanv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_DISPATCH_INDIRECT_BUFFER_BINDING = 37103


		/** Accepted by the {@code stages} parameter of UseProgramStages. */
		const val GL_COMPUTE_SHADER_BIT = 32


		/** Tokens accepted by the {@code target} parameters of Enable, Disable, and  IsEnabled. */
		const val GL_DEBUG_OUTPUT = 37600
		const val GL_DEBUG_OUTPUT_SYNCHRONOUS = 33346


		/** Returned by GetIntegerv when {@code pname} is CONTEXT_FLAGS. */
		const val GL_CONTEXT_FLAG_DEBUG_BIT = 2


		/** Tokens accepted by the {@code value} parameters of GetBooleanv, GetIntegerv,  GetFloatv, GetDoublev and GetInteger64v. */
		const val GL_MAX_DEBUG_MESSAGE_LENGTH = 37187
		const val GL_MAX_DEBUG_LOGGED_MESSAGES = 37188
		const val GL_DEBUG_LOGGED_MESSAGES = 37189
		const val GL_DEBUG_NEXT_LOGGED_MESSAGE_LENGTH = 33347
		const val GL_MAX_DEBUG_GROUP_STACK_DEPTH = 33388
		const val GL_DEBUG_GROUP_STACK_DEPTH = 33389
		const val GL_MAX_LABEL_LENGTH = 33512


		/** Tokens accepted by the {@code pname} parameter of GetPointerv. */
		const val GL_DEBUG_CALLBACK_FUNCTION = 33348
		const val GL_DEBUG_CALLBACK_USER_PARAM = 33349


		/**
		* Tokens accepted or provided by the {@code source} parameters of DebugMessageControl, DebugMessageInsert and DEBUGPROC, and the {@code sources} parameter
		* of GetDebugMessageLog.
		*/
		const val GL_DEBUG_SOURCE_API = 33350
		const val GL_DEBUG_SOURCE_WINDOW_SYSTEM = 33351
		const val GL_DEBUG_SOURCE_SHADER_COMPILER = 33352
		const val GL_DEBUG_SOURCE_THIRD_PARTY = 33353
		const val GL_DEBUG_SOURCE_APPLICATION = 33354
		const val GL_DEBUG_SOURCE_OTHER = 33355


		/**
		* Tokens accepted or provided by the {@code type} parameters of DebugMessageControl, DebugMessageInsert and DEBUGPROC, and the {@code types} parameter of
		* GetDebugMessageLog.
		*/
		const val GL_DEBUG_TYPE_ERROR = 33356
		const val GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR = 33357
		const val GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR = 33358
		const val GL_DEBUG_TYPE_PORTABILITY = 33359
		const val GL_DEBUG_TYPE_PERFORMANCE = 33360
		const val GL_DEBUG_TYPE_OTHER = 33361
		const val GL_DEBUG_TYPE_MARKER = 33384


		/** Tokens accepted or provided by the {@code type} parameters of DebugMessageControl and DEBUGPROC, and the {@code types} parameter of GetDebugMessageLog. */
		const val GL_DEBUG_TYPE_PUSH_GROUP = 33385
		const val GL_DEBUG_TYPE_POP_GROUP = 33386


		/**
		* Tokens accepted or provided by the {@code severity} parameters of DebugMessageControl, DebugMessageInsert and DEBUGPROC callback functions, and the
		* {@code severities} parameter of GetDebugMessageLog.
		*/
		const val GL_DEBUG_SEVERITY_HIGH = 37190
		const val GL_DEBUG_SEVERITY_MEDIUM = 37191
		const val GL_DEBUG_SEVERITY_LOW = 37192
		const val GL_DEBUG_SEVERITY_NOTIFICATION = 33387


		/** Tokens accepted or provided by the {@code identifier} parameters of ObjectLabel and GetObjectLabel. */
		const val GL_BUFFER = 33504
		const val GL_SHADER = 33505
		const val GL_PROGRAM = 33506
		const val GL_QUERY = 33507
		const val GL_PROGRAM_PIPELINE = 33508
		const val GL_SAMPLER = 33510
		const val GL_DISPLAY_LIST = 33511


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, GetDoublev, and GetInteger64v. */
		const val GL_MAX_UNIFORM_LOCATIONS = 33390


		/**
		* Accepted by the {@code pname} parameter of FramebufferParameteri, GetFramebufferParameteriv, NamedFramebufferParameteriEXT, and
		* GetNamedFramebufferParameterivEXT.
		*/
		const val GL_FRAMEBUFFER_DEFAULT_WIDTH = 37648
		const val GL_FRAMEBUFFER_DEFAULT_HEIGHT = 37649
		const val GL_FRAMEBUFFER_DEFAULT_LAYERS = 37650
		const val GL_FRAMEBUFFER_DEFAULT_SAMPLES = 37651
		const val GL_FRAMEBUFFER_DEFAULT_FIXED_SAMPLE_LOCATIONS = 37652


		/** Accepted by the {@code pname} parameter of GetIntegerv, GetBooleanv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_MAX_FRAMEBUFFER_WIDTH = 37653
		const val GL_MAX_FRAMEBUFFER_HEIGHT = 37654
		const val GL_MAX_FRAMEBUFFER_LAYERS = 37655
		const val GL_MAX_FRAMEBUFFER_SAMPLES = 37656


		/** Accepted by the {@code pname} parameter of GetInternalformativ and GetInternalformati64v. */
		const val GL_INTERNALFORMAT_SUPPORTED = 33391
		const val GL_INTERNALFORMAT_PREFERRED = 33392
		const val GL_INTERNALFORMAT_RED_SIZE = 33393
		const val GL_INTERNALFORMAT_GREEN_SIZE = 33394
		const val GL_INTERNALFORMAT_BLUE_SIZE = 33395
		const val GL_INTERNALFORMAT_ALPHA_SIZE = 33396
		const val GL_INTERNALFORMAT_DEPTH_SIZE = 33397
		const val GL_INTERNALFORMAT_STENCIL_SIZE = 33398
		const val GL_INTERNALFORMAT_SHARED_SIZE = 33399
		const val GL_INTERNALFORMAT_RED_TYPE = 33400
		const val GL_INTERNALFORMAT_GREEN_TYPE = 33401
		const val GL_INTERNALFORMAT_BLUE_TYPE = 33402
		const val GL_INTERNALFORMAT_ALPHA_TYPE = 33403
		const val GL_INTERNALFORMAT_DEPTH_TYPE = 33404
		const val GL_INTERNALFORMAT_STENCIL_TYPE = 33405
		const val GL_MAX_WIDTH = 33406
		const val GL_MAX_HEIGHT = 33407
		const val GL_MAX_DEPTH = 33408
		const val GL_MAX_LAYERS = 33409
		const val GL_MAX_COMBINED_DIMENSIONS = 33410
		const val GL_COLOR_COMPONENTS = 33411
		const val GL_DEPTH_COMPONENTS = 33412
		const val GL_STENCIL_COMPONENTS = 33413
		const val GL_COLOR_RENDERABLE = 33414
		const val GL_DEPTH_RENDERABLE = 33415
		const val GL_STENCIL_RENDERABLE = 33416
		const val GL_FRAMEBUFFER_RENDERABLE = 33417
		const val GL_FRAMEBUFFER_RENDERABLE_LAYERED = 33418
		const val GL_FRAMEBUFFER_BLEND = 33419
		const val GL_READ_PIXELS = 33420
		const val GL_READ_PIXELS_FORMAT = 33421
		const val GL_READ_PIXELS_TYPE = 33422
		const val GL_TEXTURE_IMAGE_FORMAT = 33423
		const val GL_TEXTURE_IMAGE_TYPE = 33424
		const val GL_GET_TEXTURE_IMAGE_FORMAT = 33425
		const val GL_GET_TEXTURE_IMAGE_TYPE = 33426
		const val GL_MIPMAP = 33427
		const val GL_MANUAL_GENERATE_MIPMAP = 33428
		const val GL_AUTO_GENERATE_MIPMAP = 33429
		const val GL_COLOR_ENCODING = 33430
		const val GL_SRGB_READ = 33431
		const val GL_SRGB_WRITE = 33432
		const val GL_FILTER = 33434
		const val GL_VERTEX_TEXTURE = 33435
		const val GL_TESS_CONTROL_TEXTURE = 33436
		const val GL_TESS_EVALUATION_TEXTURE = 33437
		const val GL_GEOMETRY_TEXTURE = 33438
		const val GL_FRAGMENT_TEXTURE = 33439
		const val GL_COMPUTE_TEXTURE = 33440
		const val GL_TEXTURE_SHADOW = 33441
		const val GL_TEXTURE_GATHER = 33442
		const val GL_TEXTURE_GATHER_SHADOW = 33443
		const val GL_SHADER_IMAGE_LOAD = 33444
		const val GL_SHADER_IMAGE_STORE = 33445
		const val GL_SHADER_IMAGE_ATOMIC = 33446
		const val GL_IMAGE_TEXEL_SIZE = 33447
		const val GL_IMAGE_COMPATIBILITY_CLASS = 33448
		const val GL_IMAGE_PIXEL_FORMAT = 33449
		const val GL_IMAGE_PIXEL_TYPE = 33450
		const val GL_SIMULTANEOUS_TEXTURE_AND_DEPTH_TEST = 33452
		const val GL_SIMULTANEOUS_TEXTURE_AND_STENCIL_TEST = 33453
		const val GL_SIMULTANEOUS_TEXTURE_AND_DEPTH_WRITE = 33454
		const val GL_SIMULTANEOUS_TEXTURE_AND_STENCIL_WRITE = 33455
		const val GL_TEXTURE_COMPRESSED_BLOCK_WIDTH = 33457
		const val GL_TEXTURE_COMPRESSED_BLOCK_HEIGHT = 33458
		const val GL_TEXTURE_COMPRESSED_BLOCK_SIZE = 33459
		const val GL_CLEAR_BUFFER = 33460
		const val GL_TEXTURE_VIEW = 33461
		const val GL_VIEW_COMPATIBILITY_CLASS = 33462


		/** Returned as possible responses for various {@code pname} queries to GetInternalformativ and GetInternalformati64v. */
		const val GL_FULL_SUPPORT = 33463
		const val GL_CAVEAT_SUPPORT = 33464
		const val GL_IMAGE_CLASS_4_X_32 = 33465
		const val GL_IMAGE_CLASS_2_X_32 = 33466
		const val GL_IMAGE_CLASS_1_X_32 = 33467
		const val GL_IMAGE_CLASS_4_X_16 = 33468
		const val GL_IMAGE_CLASS_2_X_16 = 33469
		const val GL_IMAGE_CLASS_1_X_16 = 33470
		const val GL_IMAGE_CLASS_4_X_8 = 33471
		const val GL_IMAGE_CLASS_2_X_8 = 33472
		const val GL_IMAGE_CLASS_1_X_8 = 33473
		const val GL_IMAGE_CLASS_11_11_10 = 33474
		const val GL_IMAGE_CLASS_10_10_10_2 = 33475
		const val GL_VIEW_CLASS_128_BITS = 33476
		const val GL_VIEW_CLASS_96_BITS = 33477
		const val GL_VIEW_CLASS_64_BITS = 33478
		const val GL_VIEW_CLASS_48_BITS = 33479
		const val GL_VIEW_CLASS_32_BITS = 33480
		const val GL_VIEW_CLASS_24_BITS = 33481
		const val GL_VIEW_CLASS_16_BITS = 33482
		const val GL_VIEW_CLASS_8_BITS = 33483
		const val GL_VIEW_CLASS_S3TC_DXT1_RGB = 33484
		const val GL_VIEW_CLASS_S3TC_DXT1_RGBA = 33485
		const val GL_VIEW_CLASS_S3TC_DXT3_RGBA = 33486
		const val GL_VIEW_CLASS_S3TC_DXT5_RGBA = 33487
		const val GL_VIEW_CLASS_RGTC1_RED = 33488
		const val GL_VIEW_CLASS_RGTC2_RG = 33489
		const val GL_VIEW_CLASS_BPTC_UNORM = 33490
		const val GL_VIEW_CLASS_BPTC_FLOAT = 33491


		/**
		* Accepted by the {@code programInterface} parameter of GetProgramInterfaceiv, GetProgramResourceIndex, GetProgramResourceName, GetProgramResourceiv,
		* GetProgramResourceLocation, and GetProgramResourceLocationIndex.
		*/
		const val GL_UNIFORM = 37601
		const val GL_UNIFORM_BLOCK = 37602
		const val GL_PROGRAM_INPUT = 37603
		const val GL_PROGRAM_OUTPUT = 37604
		const val GL_BUFFER_VARIABLE = 37605
		const val GL_SHADER_STORAGE_BLOCK = 37606
		const val GL_VERTEX_SUBROUTINE = 37608
		const val GL_TESS_CONTROL_SUBROUTINE = 37609
		const val GL_TESS_EVALUATION_SUBROUTINE = 37610
		const val GL_GEOMETRY_SUBROUTINE = 37611
		const val GL_FRAGMENT_SUBROUTINE = 37612
		const val GL_COMPUTE_SUBROUTINE = 37613
		const val GL_VERTEX_SUBROUTINE_UNIFORM = 37614
		const val GL_TESS_CONTROL_SUBROUTINE_UNIFORM = 37615
		const val GL_TESS_EVALUATION_SUBROUTINE_UNIFORM = 37616
		const val GL_GEOMETRY_SUBROUTINE_UNIFORM = 37617
		const val GL_FRAGMENT_SUBROUTINE_UNIFORM = 37618
		const val GL_COMPUTE_SUBROUTINE_UNIFORM = 37619
		const val GL_TRANSFORM_FEEDBACK_VARYING = 37620


		/** Accepted by the {@code pname} parameter of GetProgramInterfaceiv. */
		const val GL_ACTIVE_RESOURCES = 37621
		const val GL_MAX_NAME_LENGTH = 37622
		const val GL_MAX_NUM_ACTIVE_VARIABLES = 37623
		const val GL_MAX_NUM_COMPATIBLE_SUBROUTINES = 37624


		/** Accepted in the {@code props} array of GetProgramResourceiv. */
		const val GL_NAME_LENGTH = 37625
		const val GL_TYPE = 37626
		const val GL_ARRAY_SIZE = 37627
		const val GL_OFFSET = 37628
		const val GL_BLOCK_INDEX = 37629
		const val GL_ARRAY_STRIDE = 37630
		const val GL_MATRIX_STRIDE = 37631
		const val GL_IS_ROW_MAJOR = 37632
		const val GL_ATOMIC_COUNTER_BUFFER_INDEX = 37633
		const val GL_BUFFER_BINDING = 37634
		const val GL_BUFFER_DATA_SIZE = 37635
		const val GL_NUM_ACTIVE_VARIABLES = 37636
		const val GL_ACTIVE_VARIABLES = 37637
		const val GL_REFERENCED_BY_VERTEX_SHADER = 37638
		const val GL_REFERENCED_BY_TESS_CONTROL_SHADER = 37639
		const val GL_REFERENCED_BY_TESS_EVALUATION_SHADER = 37640
		const val GL_REFERENCED_BY_GEOMETRY_SHADER = 37641
		const val GL_REFERENCED_BY_FRAGMENT_SHADER = 37642
		const val GL_REFERENCED_BY_COMPUTE_SHADER = 37643
		const val GL_TOP_LEVEL_ARRAY_SIZE = 37644
		const val GL_TOP_LEVEL_ARRAY_STRIDE = 37645
		const val GL_LOCATION = 37646
		const val GL_LOCATION_INDEX = 37647
		const val GL_IS_PER_PATCH = 37607


		/** Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, and GetBufferPointerv. */
		const val GL_SHADER_STORAGE_BUFFER = 37074


		/**
		* Accepted by the {@code pname} parameter of GetIntegerv, GetIntegeri_v, GetBooleanv, GetInteger64v, GetFloatv, GetDoublev, GetBooleani_v, GetIntegeri_v,
		* GetFloati_v, GetDoublei_v, and GetInteger64i_v.
		*/
		const val GL_SHADER_STORAGE_BUFFER_BINDING = 37075


		/** Accepted by the {@code pname} parameter of GetIntegeri_v, GetBooleani_v, GetIntegeri_v, GetFloati_v, GetDoublei_v, and GetInteger64i_v. */
		const val GL_SHADER_STORAGE_BUFFER_START = 37076
		const val GL_SHADER_STORAGE_BUFFER_SIZE = 37077


		/** Accepted by the {@code pname} parameter of GetIntegerv, GetBooleanv, GetInteger64v, GetFloatv, and GetDoublev. */
		const val GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS = 37078
		const val GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS = 37079
		const val GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS = 37080
		const val GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS = 37081
		const val GL_MAX_FRAGMENT_SHADER_STORAGE_BLOCKS = 37082
		const val GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS = 37083
		const val GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS = 37084
		const val GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = 37085
		const val GL_MAX_SHADER_STORAGE_BLOCK_SIZE = 37086
		const val GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT = 37087


		/** Accepted in the {@code barriers} bitfield in glMemoryBarrier. */
		const val GL_SHADER_STORAGE_BARRIER_BIT = 8192


		/** Alias for the existing token MAX_COMBINED_IMAGE_UNITS_AND_FRAGMENT_OUTPUTS. */
		const val GL_MAX_COMBINED_SHADER_OUTPUT_RESOURCES = 36665


		/** Accepted by the {@code pname} parameter of TexParameter* and GetTexParameter*. */
		const val GL_DEPTH_STENCIL_TEXTURE_MODE = 37098


		/** Accepted by the {@code pname} parameter of GetTexLevelParameter. */
		const val GL_TEXTURE_BUFFER_OFFSET = 37277
		const val GL_TEXTURE_BUFFER_SIZE = 37278


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT = 37279


		/** Accepted by the {@code pname} parameters of GetTexParameterfv and  GetTexParameteriv. */
		const val GL_TEXTURE_VIEW_MIN_LEVEL = 33499
		const val GL_TEXTURE_VIEW_NUM_LEVELS = 33500
		const val GL_TEXTURE_VIEW_MIN_LAYER = 33501
		const val GL_TEXTURE_VIEW_NUM_LAYERS = 33502


		/** Accepted by the {@code pname} parameter of GetVertexAttrib*v. */
		const val GL_VERTEX_ATTRIB_BINDING = 33492
		const val GL_VERTEX_ATTRIB_RELATIVE_OFFSET = 33493


		/** Accepted by the {@code target} parameter of GetBooleani_v, GetIntegeri_v, GetFloati_v, GetDoublei_v, and GetInteger64i_v. */
		const val GL_VERTEX_BINDING_DIVISOR = 33494
		const val GL_VERTEX_BINDING_OFFSET = 33495
		const val GL_VERTEX_BINDING_STRIDE = 33496
		const val GL_VERTEX_BINDING_BUFFER = 36687


		/** Accepted by the {@code pname} parameter of GetIntegerv, .... */
		const val GL_MAX_VERTEX_ATTRIB_RELATIVE_OFFSET = 33497
		const val GL_MAX_VERTEX_ATTRIB_BINDINGS = 33498


		/** Implementation-dependent state which constrains the maximum value of stride parameters to vertex array pointer-setting commands. */
		const val GL_MAX_VERTEX_ATTRIB_STRIDE = 33509


		/**
		* Implementations are not required to support primitive restart for separate patch primitives (primitive type PATCHES). Support can be queried by calling
		* GetBooleanv with the symbolic constant PRIMITIVE_RESTART_FOR_PATCHES_SUPPORTED. A value of FALSE indicates that primitive restart is treated as
		* disabled when drawing patches, no matter the value of the enables. A value of TRUE indicates that primitive restart behaves normally for patches.
		*/
		const val GL_PRIMITIVE_RESTART_FOR_PATCHES_SUPPORTED = 33313


		/** Equivalent to {@link ARBTextureBufferObject#GL_TEXTURE_BUFFER_ARB TEXTURE_BUFFER_ARB} query, but named more consistently. */
		const val GL_TEXTURE_BUFFER_BINDING = 35882


		/** Accepted in the {@code flags} parameter of {@link #glBufferStorage BufferStorage} and {@link ARBBufferStorage#glNamedBufferStorageEXT NamedBufferStorageEXT}. */
		const val GL_MAP_PERSISTENT_BIT = 64
		const val GL_MAP_COHERENT_BIT = 128
		const val GL_DYNAMIC_STORAGE_BIT = 256
		const val GL_CLIENT_STORAGE_BIT = 512


		/** Accepted by the {@code pname} parameter of {@code GetBufferParameter&#123;indicesBuffer|i64&#125;v}. */
		const val GL_BUFFER_IMMUTABLE_STORAGE = 33311
		const val GL_BUFFER_STORAGE_FLAGS = 33312


		/** Accepted by the {@code barriers} parameter of {@link GL42#glMemoryBarrier MemoryBarrier}. */
		const val GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT = 16384


		/** Accepted by the {@code pname} parameter for {@link GL42#glGetInternalformativ GetInternalformativ} and {@link GL43#glGetInternalformati64v GetInternalformati64v}. */
		const val GL_CLEAR_TEXTURE = 37733


		/** Accepted in the {@code props} array of {@link GL43#glGetProgramResourceiv GetProgramResourceiv}. */
		const val GL_LOCATION_COMPONENT = 37706
		const val GL_TRANSFORM_FEEDBACK_BUFFER_INDEX = 37707
		const val GL_TRANSFORM_FEEDBACK_BUFFER_STRIDE = 37708


		/** Accepted by the {@code pname} parameter of {@link GL15#glGetQueryObjectiv GetQueryObjectiv}, {@link GL15#glGetQueryObjectuiv GetQueryObjectuiv}, {@link GL33#glGetQueryObjecti64v GetQueryObjecti64v} and {@link GL33#glGetQueryObjectui64v GetQueryObjectui64v}. */
		const val GL_QUERY_RESULT_NO_WAIT = 37268


		/**
		* Accepted by the {@code target} parameter of {@link GL15#glBindBuffer BindBuffer}, {@link GL15#glBufferData BufferData}, {@link GL15#glBufferSubData BufferSubData},
		* {@link GL15#glMapBuffer MapBuffer}, {@link GL15#glUnmapBuffer UnmapBuffer}, {@link GL30#glMapBufferRange MapBufferRange}, {@link GL15#glGetBufferSubData GetBufferSubData},
		* {@link GL15#glGetBufferParameteriv GetBufferParameteriv}, {@link GL32#glGetBufferParameteri64v GetBufferParameteri64v}, {@link GL15#glGetBufferPointerv GetBufferPointerv},
		* {@link GL43#glClearBufferSubData ClearBufferSubData}, and the {@code readtarget} and {@code writetarget} parameters of {@link GL31#glCopyBufferSubData CopyBufferSubData}.
		*/
		const val GL_QUERY_BUFFER = 37266


		/**
		* Accepted by the {@code pname} parameter of {@link GL11#glGetBooleanv GetBooleanv}, {@link GL11#glGetIntegerv GetIntegerv}, {@link GL11#glGetFloatv GetFloatv},
		* and {@link GL11#glGetDoublev GetDoublev}.
		*/
		const val GL_QUERY_BUFFER_BINDING = 37267


		/** Accepted in the {@code barriers} bitfield in {@link GL42#glMemoryBarrier MemoryBarrier}. */
		const val GL_QUERY_BUFFER_BARRIER_BIT = 32768


		/**
		* Accepted by the {@code param} parameter of TexParameter{if}, SamplerParameter{if} and SamplerParameter{if}v, and by the {@code params} parameter of
		* TexParameter{if}v, TexParameterI{indicesBuffer ui}v and SamplerParameterI{indicesBuffer ui}v when their {@code pname} parameter is {@link GL11#GL_TEXTURE_WRAP_S TEXTURE_WRAP_S}, {@link GL11#GL_TEXTURE_WRAP_T TEXTURE_WRAP_T}, or
		* {@link GL12#GL_TEXTURE_WRAP_R TEXTURE_WRAP_R},
		*/
		const val GL_MIRROR_CLAMP_TO_EDGE = 34627


		/** Accepted by the {@code depth} parameter of {@link #glClipControl ClipControl}. */
		const val GL_NEGATIVE_ONE_TO_ONE = 37726
		const val GL_ZERO_TO_ONE = 37727


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_CLIP_ORIGIN = 37724
		const val GL_CLIP_DEPTH_MODE = 37725


		/** Accepted by the {@code mode} parameter of {@link GL30#glBeginConditionalRender BeginConditionalRender}. */
		const val GL_QUERY_WAIT_INVERTED = 36375
		const val GL_QUERY_NO_WAIT_INVERTED = 36376
		const val GL_QUERY_BY_REGION_WAIT_INVERTED = 36377
		const val GL_QUERY_BY_REGION_NO_WAIT_INVERTED = 36378


		/** Accepted by the {@code pname} parameter of GetBooleanv, GetDoublev, GetFloatv, GetIntegerv, and GetInteger64v. */
		const val GL_MAX_CULL_DISTANCES = 33529
		const val GL_MAX_COMBINED_CLIP_AND_CULL_DISTANCES = 33530


		/** Accepted by the {@code pname} parameter of GetTextureParameter{if}v and GetTextureParameterI{indicesBuffer ui}v. */
		const val GL_TEXTURE_TARGET = 4102


		/** Accepted by the {@code pname} parameter of GetQueryObjectiv. */
		const val GL_QUERY_TARGET = 33514


		/** Accepted by the {@code pname} parameter of GetIntegerv, GetFloatv, GetBooleanv GetDoublev and GetInteger64v. */
		const val GL_CONTEXT_RELEASE_BEHAVIOR = 33531


		/** Returned in {@code data} by GetIntegerv, GetFloatv, GetBooleanv GetDoublev and GetInteger64v when {@code pname} is {@link #GL_CONTEXT_RELEASE_BEHAVIOR CONTEXT_RELEASE_BEHAVIOR}. */
		const val GL_CONTEXT_RELEASE_BEHAVIOR_FLUSH = 33532


		/** Returned by {@link #glGetGraphicsResetStatus GetGraphicsResetStatus}. */
		const val GL_GUILTY_CONTEXT_RESET = 33363
		const val GL_INNOCENT_CONTEXT_RESET = 33364
		const val GL_UNKNOWN_CONTEXT_RESET = 33365


		/** Accepted by the {@code value} parameter of GetBooleanv, GetIntegerv, and GetFloatv. */
		const val GL_RESET_NOTIFICATION_STRATEGY = 33366


		/** Returned by GetIntegerv and related simple queries when {@code value} is {@link #GL_RESET_NOTIFICATION_STRATEGY RESET_NOTIFICATION_STRATEGY}. */
		const val GL_LOSE_CONTEXT_ON_RESET = 33362
		const val GL_NO_RESET_NOTIFICATION = 33377


		/** Returned by GetIntegerv when {@code pname} is CONTEXT_FLAGS. */
		const val GL_CONTEXT_FLAG_ROBUST_ACCESS_BIT = 4


		/** Returned by {@link GL11#glGetError GetError}. */
		const val GL_CONTEXT_LOST = 1287


		/** Accepted by the {@code pname} parameters of GetBooleanv, GetIntegerv, GetFloatv, and GetDoublev. */
		const val GL_RGBA_INTEGER_MODE_EXT = 36254


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, and TexImage3D. */
		const val GL_RGBA32UI_EXT = 36208
		const val GL_RGB32UI_EXT = 36209
		const val GL_ALPHA32UI_EXT = 36210
		const val GL_INTENSITY32UI_EXT = 36211
		const val GL_LUMINANCE32UI_EXT = 36212
		const val GL_LUMINANCE_ALPHA32UI_EXT = 36213
		const val GL_RGBA16UI_EXT = 36214
		const val GL_RGB16UI_EXT = 36215
		const val GL_ALPHA16UI_EXT = 36216
		const val GL_INTENSITY16UI_EXT = 36217
		const val GL_LUMINANCE16UI_EXT = 36218
		const val GL_LUMINANCE_ALPHA16UI_EXT = 36219
		const val GL_RGBA8UI_EXT = 36220
		const val GL_RGB8UI_EXT = 36221
		const val GL_ALPHA8UI_EXT = 36222
		const val GL_INTENSITY8UI_EXT = 36223
		const val GL_LUMINANCE8UI_EXT = 36224
		const val GL_LUMINANCE_ALPHA8UI_EXT = 36225
		const val GL_RGBA32I_EXT = 36226
		const val GL_RGB32I_EXT = 36227
		const val GL_ALPHA32I_EXT = 36228
		const val GL_INTENSITY32I_EXT = 36229
		const val GL_LUMINANCE32I_EXT = 36230
		const val GL_LUMINANCE_ALPHA32I_EXT = 36231
		const val GL_RGBA16I_EXT = 36232
		const val GL_RGB16I_EXT = 36233
		const val GL_ALPHA16I_EXT = 36234
		const val GL_INTENSITY16I_EXT = 36235
		const val GL_LUMINANCE16I_EXT = 36236
		const val GL_LUMINANCE_ALPHA16I_EXT = 36237
		const val GL_RGBA8I_EXT = 36238
		const val GL_RGB8I_EXT = 36239
		const val GL_ALPHA8I_EXT = 36240
		const val GL_INTENSITY8I_EXT = 36241
		const val GL_LUMINANCE8I_EXT = 36242
		const val GL_LUMINANCE_ALPHA8I_EXT = 36243


		/** Accepted by the {@code format} parameter of TexImage1D, TexImage2D, TexImage3D, TexSubImage1D, TexSubImage2D, TexSubImage3D, DrawPixels and ReadPixels. */
		const val GL_RED_INTEGER_EXT = 36244
		const val GL_GREEN_INTEGER_EXT = 36245
		const val GL_BLUE_INTEGER_EXT = 36246
		const val GL_ALPHA_INTEGER_EXT = 36247
		const val GL_RGB_INTEGER_EXT = 36248
		const val GL_RGBA_INTEGER_EXT = 36249
		const val GL_BGR_INTEGER_EXT = 36250
		const val GL_BGRA_INTEGER_EXT = 36251
		const val GL_LUMINANCE_INTEGER_EXT = 36252
		const val GL_LUMINANCE_ALPHA_INTEGER_EXT = 36253


		/** Accepted by the {@code value} parameter of GetTexLevelParameter. */
		const val GL_TEXTURE_RED_TYPE_ARB = 35856
		const val GL_TEXTURE_GREEN_TYPE_ARB = 35857
		const val GL_TEXTURE_BLUE_TYPE_ARB = 35858
		const val GL_TEXTURE_ALPHA_TYPE_ARB = 35859
		const val GL_TEXTURE_LUMINANCE_TYPE_ARB = 35860
		const val GL_TEXTURE_INTENSITY_TYPE_ARB = 35861
		const val GL_TEXTURE_DEPTH_TYPE_ARB = 35862


		/** Returned by the {@code params} parameter of GetTexLevelParameter. */
		const val GL_UNSIGNED_NORMALIZED_ARB = 35863


		/** Accepted by the {@code internalFormat} parameter of TexImage1D, TexImage2D, and TexImage3D. */
		const val GL_RGBA32F_ARB = 34836
		const val GL_RGB32F_ARB = 34837
		const val GL_ALPHA32F_ARB = 34838
		const val GL_INTENSITY32F_ARB = 34839
		const val GL_LUMINANCE32F_ARB = 34840
		const val GL_LUMINANCE_ALPHA32F_ARB = 34841
		const val GL_RGBA16F_ARB = 34842
		const val GL_RGB16F_ARB = 34843
		const val GL_ALPHA16F_ARB = 34844
		const val GL_INTENSITY16F_ARB = 34845
		const val GL_LUMINANCE16F_ARB = 34846
		const val GL_LUMINANCE_ALPHA16F_ARB = 34847


		/**
		* Accepted by the {@code internalformat} parameter of TexImage2D, CopyTexImage2D, and CompressedTexImage2D and the {@code format} parameter of
		* CompressedTexSubImage2D.
		*/
		const val GL_COMPRESSED_RGB_S3TC_DXT1_EXT = 33776
		const val GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 33777
		const val GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 33778
		const val GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 33779


		//////////// added /////////////
		/* http://legacy.lwjgl.org/javadoc/constant-values.html */

		const val GL_VERTEX_PROGRAM_ARB = 34336

		const val GL_FRAGMENT_PROGRAM_ARB = 34820



