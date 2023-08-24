## Camera

Cross platform camera for [Gio](https://gioui.org) applications.  

# Usage

```go
camera.Open()

go func() {
  for imageResult := range camera.GetImage() {
    if imageResult.err == nil {
      // use imageOp in layout to display camera preview
      paint.NewImageOp(img)
    }
  }
}()

// you can call camera.Close() when needed

```
