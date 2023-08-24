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

func openCamera() error {
	return ErrNotAvailable
}

func closeCamera() error {
	return ErrNotAvailable
}
