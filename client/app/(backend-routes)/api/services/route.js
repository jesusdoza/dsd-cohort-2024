const API_URL = process.env.API_URL;
const LOC = "/api/services";

export async function GET(request, { params }) {
  try {
    throw new Error("error in route services");
    const data = await getServicesList();
    const body = JSON.stringify({ data });
    return new Response(body, {
      status: 200,
    });
  } catch (error) {
    const body = JSON.stringify({
      data: [],
      message: "failed to get services list",
      error: error?.message,
    });

    return new Response(body, {
      status: 500,
    });
  }
}

async function getServicesList() {
  const response = await fetch(`${API_URL}/services`);
  const data = await response.json();
  return data;
}

// Response.json({
//   Mes: "Hello, Next.js!",
//   env: process.env.API_URL,
// });

// return new Response(
//   { Mes: "Hello, Next.js!", env: process.env.API_URL },
//   {
//     status: 200,
//   }
// );
