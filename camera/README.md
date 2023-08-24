## Camera

Cross platform camera for [Gio](https://gioui.org) applications.  

# Usage

```go
camera.open()

go func() {
  for imageResult := range camera.GetImage() {
    if imageResult.err == nil {
      // use imageOp in layout to dispaly camera preview
      paint.NewImageOp(img)
    }
  }
}()

// you can call camera.close() when needed

```
