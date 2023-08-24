//go:build android
// +build android

package camera

/*
#cgo LDFLAGS: -landroid

#include <jni.h>
#include <stdlib.h>
*/
import "C"
import (
	"bytes"
	"errors"
	"image"
	"unsafe"

	"gioui.org/app"
	"gioui.org/io/event"
	"git.wow.st/gmp/jni"
)

//go:generate javac -source 8 -target 8  -bootclasspath $ANDROID_HOME/platforms/android-30/android.jar -d $TEMP/camera_camera_android/classes camera_android.java
//go:generate jar cf camera_android.jar -C $TEMP/camera_camera_android/classes .

func listenEvents(evt event.Event) {
	if evt, ok := evt.(app.ViewEvent); ok {
		view = evt.View
	}
}

func openCamera() error {
	err := jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "openCamera", "(Landroid/view/View;)V")
		err = jni.CallStaticVoidMethod(env, class, methodId, jni.Value(view))
		if err != nil {
			return err
		}

		result = make(chan ImageResult)
		return nil
	})

	return err
}

func closeCamera() error {
	err := jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "closeCamera", "()V")
		err = jni.CallStaticVoidMethod(env, class, methodId)
		if err != nil {
			return err
		}

		return nil
	})

	return err
}

func loadClass(env jni.Env) (jni.Class, error) {
	return jni.LoadClass(env, jni.ClassLoaderFor(env, jni.Object(app.AppContext())), "org/gioui/x/camera/camera_android")
}

//export Java_org_gioui_x_camera_camera_1android_ImageCallback
func Java_org_gioui_x_camera_camera_1android_ImageCallback(_env *C.JNIEnv, _ C.jclass, data C.jbyteArray, err C.jstring) {
	var res ImageResult
	env := jni.EnvFor(uintptr(unsafe.Pointer(_env)))

	if err := jni.GoString(env, jni.String(uintptr(err))); len(err) > 0 {
		res.Err = errors.New(err)
	} else {
		imageData := jni.GetByteArrayElements(env, jni.ByteArray(uintptr(data)))
		reader := bytes.NewReader(imageData)

		img, _, err := image.Decode(reader)
		if err != nil {
			res.Err = err
		} else {
			res.Image = img
		}
	}

	result <- res
}
