package promtior.booking.backend.enums;

import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;
import static java.util.ResourceBundle.Control.getNoFallbackControl;

import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.slf4j.Logger;

import promtior.booking.backend.dto.EnumResource;


public interface IEnumResource {

   String getEnumKey();

   String getResourceKey();

   Logger getLog();

   default String getPropertiesName() {
      return "promtior/booking/backend/enums/enum";
   }

   default String getLocaleMessage(Object... params) {
      return getLocaleMessageImpl(Locale.ENGLISH, params);
   }

   default String getLocaleMessageImpl(Locale locale, Object... params) {
      String resourceKeyName = getResourceKey();
      try {
         ResourceBundle rs = ResourceBundle.getBundle(getPropertiesName(), locale, getNoFallbackControl(FORMAT_PROPERTIES));
         if (Objects.nonNull(((PropertyResourceBundle) rs).handleGetObject(resourceKeyName))) {
            String message = rs.getString(resourceKeyName);
            return params.length > 0 ? format(message, params) : message;
         }
      } catch (Exception ex) {
         getLog().warn("Exception loading message for locale: {}", locale);
      }
      return resourceKeyName;
   }

   private static String format(String message, Object... params) {
      StringBuilder sb = new StringBuilder(message);
      for (Object param : params) {
         int idx = sb.indexOf("{}");
         if (idx == -1) {
            break;
         }
         sb.replace(idx, idx + 2, param != null ? param.toString() : "null");
      }
      return sb.toString();
   }

   default EnumResource getEnumResource() {
      return new EnumResource(getEnumKey(), getLocaleMessage());
   }

   default EnumResource getEnumResourceAndSetLocaleMessageLanguage(String language, Object... params) {
      Locale locale = language != null ? Locale.of(language) : Locale.ENGLISH;
      return new EnumResource(getEnumKey(), getLocaleMessageImpl(locale, params));
   }
}
