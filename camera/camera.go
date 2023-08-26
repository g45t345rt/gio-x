package camera

import (
	"image"

	"gioui.org/io/event"
)

var (
	view       uintptr
	feedResult chan FeedResult
)

type FeedResult struct {
	Image image.Image
	Err   error
}

const (
	LensFacingFront    = "FRONT"
	LensFacingBack     = "BACK"
	LensFacingExternal = "EXTERNAL"
)

func ListenEvents(evt event.Event) {
	listenEvents(evt)
}

func GetSensorOrientation(cameraId string) (int, error) {
	return getCameraSensorOrientation(cameraId)
}

func GetLensFacing(cameraId string) (string, error) {
	return getCameraLensFacing(cameraId)
}

func GetIdList() ([]string, error) {
	return getCameraIdList()
}

func OpenFeed(cameraId string, width int, height int) error {
	return openCameraFeed(cameraId, width, height)
}

func CloseFeed() error {
	return closeCameraFeed()
}

func GetFeed() chan FeedResult {
	return feedResult
}
