from pydantic import BaseModel, EmailStr


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str
    name: str
    stance: str = "ORTHODOX"
    weight_kg: float | None = None
    height_cm: float | None = None
    experience_level: str = "BEGINNER"


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    id: str
    email: str
    name: str
    stance: str
    weight_kg: float | None
    height_cm: float | None
    experience_level: str

    model_config = {"from_attributes": True}
