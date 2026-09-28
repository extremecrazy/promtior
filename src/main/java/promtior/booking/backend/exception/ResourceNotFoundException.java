package promtior.booking.backend.exception;


public class ResourceNotFoundException extends RuntimeException {

   public ResourceNotFoundException(String message) {
      super(message);
   }
}
