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

func loadCameraClass(env jni.Env) (jni.Class, error) {
	return jni.LoadClass(env, jni.ClassLoaderFor(env, jni.Object(app.AppContext())), "org/gioui/x/camera/camera_android")
}

func openCameraFeed(cameraId string, width int, height int) error {
	err := jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadCameraClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "openCameraFeed", "(Landroid/view/View;Ljava/lang/String;II)V")
		err = jni.CallStaticVoidMethod(env, class, methodId, jni.Value(view), jni.Value(jni.JavaString(env, cameraId)), jni.Value(width), jni.Value(height))
		if err != nil {
			return err
		}

		feedResult = make(chan FeedResult)
		return nil
	})

	return err
}

func getCameraIdList() ([]string, error) {
	var ids []string

	err := jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadCameraClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "getCameraIdList", "(Landroid/content/Context;)[Ljava/lang/String;")
		obj, err := jni.CallStaticObjectMethod(env, class, methodId, jni.Value(app.AppContext()))
		if err != nil {
			return err
		}

		arrayObject := jni.ObjectArray(obj)
		// _jni_GetArrayLength is not exported -_-
		// quick hack with max loop of 10 that break when index is out of bound
		for i := 0; i < 10; i++ {
			elm, err := jni.GetObjectArrayElement(env, arrayObject, jni.Size(i))
			if err != nil {
				break
			}

			value := jni.GoString(env, jni.String(elm))
			ids = append(ids, value)
		}

		return nil
	})

	return ids, err
}

func getCameraSensorOrientation(cameraId string) (orientation int, err error) {
	err = jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadCameraClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "getCameraSensorOrientation", "(Landroid/content/Context;Ljava/lang/String;)I")
		orientation, err = jni.CallStaticIntMethod(env, class, methodId, jni.Value(app.AppContext()), jni.Value(jni.JavaString(env, cameraId)))
		if err != nil {
			return err
		}

		return nil
	})

	return orientation, err
}

func getCameraLensFacing(cameraId string) (lensFacing string, err error) {
	err = jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadCameraClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "getCameraLensFacing", "(Landroid/content/Context;Ljava/lang/String;)I")
		value, err := jni.CallStaticIntMethod(env, class, methodId, jni.Value(app.AppContext()), jni.Value(jni.JavaString(env, cameraId)))
		if err != nil {
			return err
		}

		switch value {
		case 0:
			lensFacing = "FRONT"
		case 1:
			lensFacing = "BACK"
		case 2:
			lensFacing = "EXTERNAL"
		}

		return nil
	})

	return lensFacing, err
}

func closeCameraFeed() error {
	err := jni.Do(jni.JVMFor(app.JavaVM()), func(env jni.Env) error {
		class, err := loadCameraClass(env)
		if err != nil {
			return err
		}

		methodId := jni.GetStaticMethodID(env, class, "closeCameraFeed", "()V")
		err = jni.CallStaticVoidMethod(env, class, methodId)
		if err != nil {
			return err
		}

		return nil
	})

	return err
}

//export Java_org_gioui_x_camera_camera_1android_FeedCallback
func Java_org_gioui_x_camera_camera_1android_FeedCallback(_env *C.JNIEnv, _ C.jclass, data C.jbyteArray, err C.jstring) {
	var res FeedResult
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

	feedResult <- res
}
