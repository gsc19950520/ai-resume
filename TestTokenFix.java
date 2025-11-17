import com.aicv.airesume.utils.TokenUtils;

public class TestTokenFix {
    public static void main(String[] args) {
        // 模拟测试
        TokenUtils tokenUtils = new TokenUtils();
        
        // 设置测试用的密钥
        String secret = "ai_resume_optimizer_secret_key";
        
        // 测试用户ID
        Long userId = 43L;
        
        System.out.println("=== Token生成和解析测试 ===");
        System.out.println("用户ID: " + userId);
        
        // 生成JWT token (模拟UserServiceImpl修改后的行为)
        String jwtToken = tokenUtils.generateToken(userId);
        System.out.println("生成的JWT Token: " + jwtToken);
        
        // 解析JWT token (模拟TokenUtils的行为)
        Long parsedUserId = tokenUtils.getUserIdFromToken(jwtToken);
        System.out.println("解析出的用户ID: " + parsedUserId);
        
        // 测试旧的token格式
        String oldToken = "token_43_1763382187363_373628";
        System.out.println("\n=== 旧token格式测试 ===");
        System.out.println("旧的token格式: " + oldToken);
        Long oldParsedUserId = tokenUtils.getUserIdFromToken(oldToken);
        System.out.println("旧token解析结果: " + oldParsedUserId);
        
        System.out.println("\n=== 测试结果分析 ===");
        if (parsedUserId != null && parsedUserId.equals(userId)) {
            System.out.println("✅ JWT Token生成和解析正常");
        } else {
            System.out.println("❌ JWT Token生成或解析失败");
        }
        
        if (oldParsedUserId == null) {
            System.out.println("✅ 旧token格式被正确拒绝 (返回null)");
        } else {
            System.out.println("❌ 旧token格式未被正确拒绝");
        }
    }
}