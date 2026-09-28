package promtior.booking.backend.controller;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import promtior.booking.backend.dto.chat.ChatRequest;
import promtior.booking.backend.dto.chat.ChatResponse;
import promtior.booking.backend.entity.User;
import promtior.booking.backend.tool.ChatToolsService;

@Controller
@RequiredArgsConstructor
public class ChatController {

   private final ChatClient chatClient;

   @GetMapping("/chat")
   public String chat(@AuthenticationPrincipal User currentUser, Model model) {
      model.addAttribute("username", currentUser.getName());
      model.addAttribute("isAdmin", currentUser.isAdmin());
      return "chat";
   }

   @ResponseBody
   @PostMapping("/chat/messages")
   public ChatResponse sendMessage(@Valid @RequestBody ChatRequest request, HttpSession session) {
      String reply = chatClient.prompt()
            .user(request.message())
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, session.getId()))
            .toolContext(Map.of(ChatToolsService.BOOKING_CREATED_KEY, new AtomicBoolean(false)))
            .call()
            .content();
      return new ChatResponse(reply);
   }
}
