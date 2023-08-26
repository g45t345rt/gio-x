## Camera

Cross platform camera for [Gio](https://gioui.org) applications.  

# Usage

```go
camera.GetIdList() // get list of available camera ids e.g ["0","1","2","3"]
camera.GetLensFacing("0") // get camera lens facing location (FRONT,BACK,EXTERNAL)
camera.GetSensorOrientation("0") // get camera sensor orientation (0,90,180,270) - needed if you want to rotate the image feed correctly

camera.OpenFeed("0", 256, 256) // cameraId, width, height

go func() {
  for feedResult := range camera.GetFeed() {
    if feedResult.err == nil {
      // use imageOp in layout to display camera preview
      paint.NewImageOp(feedResult.image)
    }
  }
}()

// you can call camera.CloseFeed() when needed

```
