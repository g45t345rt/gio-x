//go:build !android
// +build !android

package camera

import (
	"errors"

	"gioui.org/io/event"
)

var (
	ErrNotAvailable = errors.New("current OS not supported")
)

func listenEvents(_ event.Event) {}

func getCameraSensorOrientation(cameraId string) (int, error) {
	return 0, ErrNotAvailable
}

func getCameraLensFacing(cameraId string) (string, error) {
	return "", ErrNotAvailable
}

func getCameraIdList() ([]string, error) {
	return nil, ErrNotAvailable
}

func openCameraFeed(cameraId string, width int, height int) error {
	return ErrNotAvailable
}

func closeCameraFeed() error {
	return ErrNotAvailable
}
