package camera

import (
	"image"

	"gioui.org/io/event"
)

var (
	view   uintptr
	result chan ImageResult
)

type ImageResult struct {
	Image image.Image
	Err   error
}

func ListenEvents(evt event.Event) {
	listenEvents(evt)
}

func Open() error {
	return openCamera()
}

func Close() error {
	return closeCamera()
}

func GetImage() chan ImageResult {
	return result
}
