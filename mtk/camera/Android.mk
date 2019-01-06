// libcamera_parameters_mtk
//	by: daniel_hk (https://github.com/danielhk)

cc_library_static {
    name: "libcamera_parameters_mtk",

    srcs: ["CameraParameters.cpp"],
    export_include_dirs: ["."],

    include_dirs: [
        "device/xiaomi/hermes/include",
    ]
}
